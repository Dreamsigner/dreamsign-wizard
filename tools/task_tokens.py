"""Read Codex native counters and maintain a task ledger without publishing transcripts."""
import argparse
import json
import os
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / "build/task-tokens/state.json"
LEDGER = ROOT / "docs/metrics/task-tokens.json"
REPORT = ROOT / "docs/TOKEN-USAGE.md"
FIELDS = ("input_tokens", "cached_input_tokens", "output_tokens", "reasoning_output_tokens", "total_tokens")


def read_usage(path, marker=None):
    latest = baseline = None
    model = "unknown"
    matched = marker is None
    with Path(path).open(encoding="utf-8") as stream:
        for line in stream:
            try:
                row = json.loads(line)
            except json.JSONDecodeError:
                continue  # A live writer may not have finished the last line.
            payload = row.get("payload", {})
            if row.get("type") == "turn_context":
                model = payload.get("model", model)
            if row.get("type") == "response_item" and payload.get("role") == "user" and marker:
                content = " ".join(c.get("text", "") for c in payload.get("content", []) if isinstance(c, dict))
                if marker in content:
                    baseline = latest
                    matched = True
            if row.get("type") == "event_msg" and payload.get("type") == "token_count":
                info = payload.get("info") or {}
                usage = info.get("total_token_usage")
                if usage:
                    latest = {"at": row["timestamp"], "usage": {k: int(usage.get(k, 0)) for k in FIELDS}, "model": model}
    if not matched or not latest or (marker and not baseline):
        raise ValueError("No native counter or task boundary found; consumption is unavailable.")
    return baseline if marker else latest


def usage_delta(start, end):
    result = {k: end["usage"][k] - start["usage"][k] for k in FIELDS}
    if any(v < 0 for v in result.values()):
        raise ValueError("Counter reset detected; split this task into separate measured segments.")
    result["uncached_input_tokens"] = result["input_tokens"] - result["cached_input_tokens"]
    return result


def discover_source():
    thread = os.environ.get("CODEX_THREAD_ID")
    if not thread:
        raise ValueError("CODEX_THREAD_ID missing; pass --source explicitly.")
    roots = [Path(os.environ.get("CODEX_HOME", "D:/Coding/_Codex/Home")) / "sessions", Path.home() / ".codex/sessions"]
    files = [p for root in roots if root.exists() for p in root.glob(f"*/*/*/*{thread}.jsonl")]
    if not files:
        raise ValueError("Active transcript not found; pass --source explicitly.")
    return max(files, key=lambda p: p.stat().st_mtime)


def load(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def report(ledger):
    rows = ["# Consommation de tokens par tâche", "",
            "Mesures natives Codex. Les chiffres couvrent seulement la période indiquée par chaque relevé.",
            "Une tâche reste ouverte jusqu’au relevé suivant : la réponse finale et le dernier appel ne sont pas encore comptés au moment de leur propre mesure.", "",
            "Les entrées incluent le contexte relu à chaque appel. Le cache est une partie des entrées; le raisonnement est une partie des sorties. Ne pas les additionner une seconde fois.",
            "Ces tokens ne donnent ni un coût en dollars du forfait ChatGPT ni un pourcentage de quota.", "",
            "Le relevé provient du journal actif, lu sans modification. Seuls les compteurs agrégés sont publiés : ni conversations, chemins locaux, identifiants de session ou secrets.",
            "Les snapshots sont cumulatifs dans une tâche : retenir le dernier, ne jamais les sommer. Les anciennes tâches sans bornes fiables ne sont pas reconstruites artificiellement.", "",
            "## Mise à jour du registre", "",
            "Avant une tâche : annoncer une estimation de sorties, puis `python tools/task_tokens.py begin Txxx --title 'Titre' --estimate MIN MAX`. Utiliser `--marker` pour inclure l’inspection déjà faite depuis une requête utilisateur identifiable.",
            "Pendant la tâche : `python tools/task_tokens.py checkpoint Txxx`. À la frontière suivante : `python tools/task_tokens.py close Txxx`, puis ouvrir la nouvelle tâche. Éviter les intervalles qui se chevauchent.",
            "Le script utilise le journal du thread courant ayant la modification la plus récente, ou `--source` explicite. L’état local est ignoré par Git dans `build/task-tokens`; le JSON partageable se trouve dans `docs/metrics/task-tokens.json`.", "",
            "| Tâche | État | Estimation sorties | Entrées | Cache inclus | Sorties | Raisonnement inclus | Total | Mesuré jusqu’au (UTC) |",
            "|---|---|---:|---:|---:|---:|---:|---:|---|"]
    for task in ledger["tasks"]:
        c = task.get("measured", {})
        rows.append(f"| {task['id']} · {task['title']} | {task['status']} | {task['estimated_output_min']}–{task['estimated_output_max']} | " +
                    " | ".join(str(c.get(k, "—")) for k in FIELDS) + f" | {task.get('measured_through', '—')} |")
    REPORT.write_text("\n".join(rows) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["begin", "checkpoint", "close"])
    parser.add_argument("id")
    parser.add_argument("--title")
    parser.add_argument("--estimate", nargs=2, type=int)
    parser.add_argument("--marker", help="Exact fragment of the user request; starts before its first model call.")
    parser.add_argument("--source", type=Path)
    args = parser.parse_args()
    state = load(STATE, {"tasks": {}})
    ledger = load(LEDGER, {"schemaVersion": 1, "measurement": "codex_native_cumulative_counter_delta", "tasks": []})
    if args.action == "begin":
        if args.id in state["tasks"] or any(t["status"] == "in_progress" for t in ledger["tasks"]):
            raise ValueError("Task exists or another task is active; close it first to avoid overlapping attribution.")
        if not args.title or not args.estimate or not (0 <= args.estimate[0] <= args.estimate[1]):
            raise ValueError("A title and ordered output-token estimate are required.")
        source = args.source or discover_source()
        baseline = read_usage(source, args.marker)
        state["tasks"][args.id] = {"source": str(source), "baseline": baseline}
        task = {"id": args.id, "title": args.title, "status": "in_progress", "started_at": baseline["at"],
                "estimated_output_min": args.estimate[0], "estimated_output_max": args.estimate[1], "snapshots": []}
        ledger["tasks"].append(task)
    else:
        task = next(t for t in ledger["tasks"] if t["id"] == args.id)
        if task["status"] != "in_progress":
            raise ValueError("Task is already closed.")
        entry = state["tasks"][args.id]
        latest = read_usage(Path(entry["source"]))
        measured = usage_delta(entry["baseline"], latest)
        task.update(measured=measured, measured_through=latest["at"], model=latest["model"])
        if not task["snapshots"] or task["snapshots"][-1]["at"] != latest["at"]:
            task["snapshots"].append({"at": latest["at"], "usage": measured})
        if args.action == "close":
            task["status"] = "closed_at_checkpoint"
    write(STATE, state)
    write(LEDGER, ledger)
    report(ledger)
    print(json.dumps({k: v for k, v in task.items() if k != "snapshots"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
