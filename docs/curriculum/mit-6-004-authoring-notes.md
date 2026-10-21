# MIT 6.004 — provenance et contrôle du parcours

## Édition et décompte

Édition fixée : **Computation Structures, Spring 2017**, Chris Terman, MIT OCW. Consultation : **20 octobre 2026**. Une seule unité de cours représente le groupe OSSU `mit_computation_structures` et ses références `ossu_038`, `ossu_039`, `ossu_040` ; les trois volets MITx ne sont pas recomptés comme trois cours.

- Cours : https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/
- Calendrier réellement consulté : https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/calendar/
- Syllabus réellement consulté : https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/syllabus/

Le calendrier comporte **24 conférences L1–L24**, **quatre quiz** et les échéances de laboratoires/projet. L’index pédagogique comporte **21 unités annotées**. Le manifeste déclare donc **28 séances**, dans l’ordre du calendrier : L1–L7, quiz 1, L8–L13, quiz 2, L14–L18, quiz 3, L19–L24, quiz 4. Les plages du calendrier sont conservées exactement : quiz 1 L1–L5 ; quiz 2 L6–L12 ; quiz 3 L13–L17 ; quiz 4 L18–L22. Le syllabus précise qu’il n’y a pas d’examen final.

L14 et L15 utilisent deux parties de l’unité 14 ; L16 et L17 utilisent l’unité 15. Les notes et exercices sont distincts. L24 est bien une conférence du calendrier (« Building a Better Beta: tips for the design project »), mais aucun chapitre annoté L24 distinct n’est publié dans cet index. Sa leçon annonce honnêtement une synthèse originale des unités 8, 13, 15 et 21 ; elle ne présente aucun faux support spécifique ni projet réalisé. Le titre source « Assmbly language » de L10 conserve l’orthographe du calendrier, alors que le titre français est corrigé.

## Matière lue

Lecture des **21 pages Annotated Slides**, y compris les explications en paragraphes autour des diapositives ; la matière n’a pas été déduite des seuls titres de l’index. Les sections pertinentes pour les exercices sont consignées ci-dessous et dans `readingFocus` du manifeste. Ces supports sont des pages HTML annotées ; aucun PDF de diapositives n’a été trouvé sur ces pages lors de cette consultation. Les figures seules n’ont pas fait l’objet d’une inspection visuelle exhaustive. Aucune lecture vidéo n’est revendiquée. Les empreintes portent sur les octets HTML récupérés, avant extraction du texte ; une page OCW peut évoluer et son enveloppe peut changer.

| Unité | Matière consultée et utilisée | Support précis | SHA-256 HTML |
|---|---|---|---|
| 1 | Information et entropie, codage préfixe/Huffman, complément à deux et distance de Hamming. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c1/c1s1/ | `0e103fb4e5e3f3cf9f1e3ab1a2aaed5b39f9b642f3b306148ed10234587d5d3a` |
| 2 | Discipline statique, plages de tension, marges de bruit, composition combinatoire acyclique. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c2/c2s1/ | `26fa10d4cd95df387b6307e5866f6a202643c25e9a2ff35eae8579e2088f6c2a` |
| 3 | Réseaux NFET/PFET complémentaires, NAND/NOR, délais de propagation et de contamination. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c3/c3s1/ | `414b8cda82b595dbc8a2fd4d3d9ad511e01c5b66808ab960e271328b6127a055` |
| 4 | Sommes de produits, simplification, cartes de Karnaugh, consensus/leniency, MUX et ROM. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c4/c4s1/ | `b0fee854cd93cc4c4d4d33aaf5256cbd4492b748af608e71cdbcee3c61c588ff` |
| 5 | Latch transparent, registre à front, montage maître-esclave, setup/hold et chemins min/max. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c5/c5s1/ | `5ec71573a110d95a12252d7e6f40950e66f85b4a2e43402195abf8378daf742b` |
| 6 | Transitions et équivalence de FSM, encodage des états, RoboAnt, métastabilité et quarantaine. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c6/c6s1/ | `7d8af15e374e8885e42f1756e00634a3fc34e63294ec17cd1b2396b1b142b1bf` |
| 7 | Latence/débit, contours de pipeline, équilibrage et entrelacement de ressources. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c7/c7s1/ | `937f208106c9d309b9138d0da1151644e3491e669bc8b3e9219748a95a194189` |
| 8 | Puissance statique/dynamique CMOS, carry-select/carry-lookahead, multiplication et réutilisation séquentielle. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c8/c8s1/ | `d112e7b59124965f0fde8fb59b203bde72f21807d6827be52f61daa0a6c31885` |
| 9 | Programme enregistré, registres et R31, littéraux signés, adressage octet et branches PC+4. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c9/c9s1/ | `ab238dca2141cc554e36e0046038e3b36fc17479c0b5791742abd4725f405765` |
| 10 | UASM, labels et macros/pseudo-instructions, trace de factorielle, modèles universels et arrêt. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c10/c10s1/ | `f143844718effb8d9878501add7ec36616d1e7c74a5b9ee7b4da1b3c314e22e0` |
| 11 | Compilation d’expressions/conditions/boucles, IR et optimisations préservant les dépendances. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c11/c11s1/ | `d85fb39fd14b1c96de5b4019c58d200f3641f1956e98a0a854075f789804420a` |
| 12 | Convention de pile Beta croissante, SP premier mot libre, cadres et sauvegarde de liaison. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c12/c12s1/ | `1037241782333314e6824eb9926bd3e1d8cd7a4e0c1ed1c43f683d40d1b658f0` |
| 13 | Ports du fichier de registres, ALU/LD/ST/branches, multiplexeurs et ROM de contrôle. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c13/c13s1/ | `5f6f4c3971a69c7f71e53f1c07eb59f9a076ef598dc1bb4d252728a0d7dbe0d4` |
| 14 | Technologies et localité, tag/indice/déplacement, conflits et associativité, write-back/dirty. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c14/c14s1/ | `549ce395e2514c539bb04d73216de83811727ca7c987a53c9a48a61c78e4f63b` |
| 15 | IF/RF/ALU/MEM/WB, bypass vers RF, deux stalls load-to-use, annulation des branches. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c15/c15s1/ | `00eacb494bcb6d526b67aaed27156c69eeb096eb305772152b54be0d463bac71` |
| 16 | Pagination, déplacement invariant, table plate/hiérarchique, TLB, défauts de page et contextes. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c16/c16s1/ | `fac3ee7773ad3421a8156eb34433841ab3df1aa5671067a036213aee5737bd07` |
| 17 | État des processus, interruption de timer, commutation, mode noyau et appels superviseur. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c17/c17s1/ | `187db69e836f5615f0000f0bdb36e6fc0db8ecf6b091f4f50932133b0f5c7f9c` |
| 18 | Périphériques asynchrones, tampon de clavier, attente/suspension, latence et contraintes temps réel. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c18/c18s1/ | `90958cd0ba3f3428134cc5931a9e4d7f3f574a19d634fe5e3ac28f40d9ed20ea` |
| 19 | Tampon borné, WAIT/SIGNAL non négatifs, exclusion atomique et interblocage/ordre global. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c19/c19s1/ | `66824ffe170bf293c2e6145e1108d23027c1eac79f4fb2b4aa298828341dea7d` |
| 20 | Interfaces et fils réels, bus et liens série point à point, topologies et couches réseau. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c20/c20s1/ | `acb05ad2cdc4a6b352308d6a629e19a7c21574c08336d1a80063a3d7ff811ed7` |
| 21 | ILP/vectoriel/multicœur, Amdahl, cohérence séquentielle, MESI et barrières. | https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/pages/c21/c21s1/ | `b1bcdecefba09febd9f2b88a765d715ad1f2a699d82ece4779c1d974896b6185` |

## Travail pédagogique livré

**28 leçons originales en français, 99 exercices**, trois ou quatre par leçon, dont quatre par quiz. Les notes contiennent de 105 à 127 mots selon un comptage par espaces. Questions d’application, traces, tables de vérité et arbitrages de conception ; quatre réponses distinctes, indice et correction raisonnée à chaque exercice. Deux exercices montrent du code assembleur Beta à tracer ; aucune exécution matérielle de ces extraits n’est revendiquée.

Les quiz sont des **entraînements originaux adaptés au périmètre du calendrier**, sans copie supposée des sujets de 2017, sans équivalence de notation et sans score MIT. Le calendrier et le syllabus définissent leurs périmètres, mais l’index consulté ne donne pas leurs sujets complets. Le cours campus comprend sept laboratoires et un projet : le calendrier indique explicitement que les laboratoires ne sont pas inclus dans cette édition OCW. Ces réalisations complètes ne sont ni rédigées ni revendiquées ici. Les exercices courts ne prouvent pas les compétences de construction et de débogage d’un processeur complet.

## Contrôles reproductibles

Fichier durable : `tools/check_computer_architecture.py`. Exécution hors ligne, bibliothèque standard uniquement :

```text
python tools/check_computer_architecture.py
python tools/validate_curriculum.py
```

Résultat exécuté : **60 réponses finies recalculées + 12 scénarios bornés = 72 contrôles PASS**, avec vérification de la correspondance 28 séances/99 exercices du JSON actuel. Les 60 fixtures comparent une valeur recalculée à la réponse réellement sélectionnée ; le SHA-256 de l’énoncé les lie aux hypothèses actuelles. Les petits caches sont tracés avec état et remplacement, pas validés par une constante mémorisée. Les 12 scénarios supplémentaires vérifient des tables booléennes finies, la parité, une dépendance de compilation, une pile, le blocage d’un sémaphore, les six entrelacements séquentiellement cohérents, et deux comparaisons de conception. Ils sont liés aux choix du JSON par identifiant. Le script n’exécute aucun extrait du cours et n’évalue que ses propres expressions littérales de contrôle ; les données JSON ne sont jamais exécutées comme code.

Interpréteur effectivement utilisé :
`C:\Users\The_dr3amsigner\AppData\Local\Microsoft\WindowsApps\PythonSoftwareFoundation.Python.3.12_qbz5n2kfra8p0\python.exe`.
Aucun paquet ni moteur supplémentaire installé. Le validateur de curriculum a également réussi sans option de génération. Les autres explications ont été relues par raisonnement manuel ; 72 contrôles ne signifient pas 99 preuves automatisées.

### Hypothèses contrôlées

- Timing sans skew lorsqu’indiqué ; setup sur les délais maximaux, hold sur les délais minimaux. Allonger la période ne corrige pas le hold.
- Complément à deux de largeur explicite ; R31 vaut zéro ; adresses Beta en octets, branche relative depuis PC+4 avec déplacement en mots.
- Convention Beta du support : pile croissante, SP premier mot libre ; les conventions d’autres architectures peuvent différer.
- **Pipeline du support** : opérandes requis en RF ; résultat LD disponible en WB. Un consommateur immédiat demande **deux** stalls même avec bypass complet. Aucune importation du modèle d’un autre pipeline à un seul stall.
- Pénalité de défaut explicitement supplémentaire au coût cache de base ; caches initialement vides quand l’énoncé le précise. Les accès de trace sont des numéros de mots.
- Traduction paginée conservant le déplacement ; défaut TLB distinct du défaut de page ; charge moyenne d’interruptions distincte d’une garantie d’échéance.
- WAIT atomique avec convention de sémaphore non négatif ; absence d’interblocage ne prouve pas absence de famine.
- L’exclusion du résultat de deux lectures nulles est prouvée seulement sous cohérence séquentielle, par les six ordres globaux admissibles. MESI pour une adresse et ordre global multi-adresses sont distingués.
- Amdahl calcule un temps normalisé avant inversion ; contraintes de surface appliquées avant choix de vitesse. Les considérations technologiques du support sont rattachées à l’édition 2017, sans prétention d’état de l’art 2026.

## Limites de validation

Aucune simulation analogique, exécution HDL, synthèse de portes, validation électrique, vérification de CPU complet ou expérience de laboratoire. Les petits modèles numériques reproduisent seulement les hypothèses explicitement formulées. L24 et les quiz sont des adaptations originales déclarées. Aucun build, génération Kotlin ou commit n’a été effectué dans cette tâche.
