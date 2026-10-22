import json
import tempfile
import unittest
from pathlib import Path
from task_tokens import read_usage, usage_delta

class TaskTokensTest(unittest.TestCase):
    def test_boundary_cache_reasoning_and_repeated_cumulative_snapshots(self):
        def counter(n):
            return {"timestamp": str(n), "type": "event_msg", "payload": {"type": "token_count", "info": {"total_token_usage": {
                "input_tokens": n*100, "cached_input_tokens": n*70, "output_tokens": n*10,
                "reasoning_output_tokens": n*3, "total_tokens": n*110}}}}
        rows = [counter(1), {"type": "response_item", "payload": {"role": "user", "content": [{"text": "new task"}]}}, counter(2), counter(2), counter(3)]
        with tempfile.TemporaryDirectory() as d:
            p = Path(d)/"session.jsonl"
            p.write_text("\n".join(json.dumps(r) for r in rows)+"\n{unfinished", encoding="utf-8")
            delta = usage_delta(read_usage(p, "new task"), read_usage(p))
            self.assertEqual(220, delta["total_tokens"])
            self.assertEqual(140, delta["cached_input_tokens"])
            self.assertEqual(20, delta["output_tokens"])
            self.assertEqual(6, delta["reasoning_output_tokens"])
            self.assertEqual(60, delta["uncached_input_tokens"])
            with self.assertRaises(ValueError): read_usage(p, "missing")
    def test_counter_reset_is_not_silently_reported_as_consumption(self):
        with self.assertRaises(ValueError):
            usage_delta({"usage": {k: 100 for k in ("input_tokens", "cached_input_tokens", "output_tokens", "reasoning_output_tokens", "total_tokens")}},
                        {"usage": {k: 1 for k in ("input_tokens", "cached_input_tokens", "output_tokens", "reasoning_output_tokens", "total_tokens")}})
