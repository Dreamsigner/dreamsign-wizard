# Rédaction — Stanford CS364A, Algorithmic Game Theory (Fall 2013)

Référence OSSU `ossu_043`; édition retenue : **Fall 2013**, Stanford University, CS364A, Tim Roughgarden. L’index officiel annonce 20 conférences. Le manifeste conserve l’ordre, les numéros, les titres et les liens individuels.

## Lecture des sources

Le 20 octobre 2026, les vingt PDF officiels ont été récupérés (HTTP 200) et toutes leurs pages ont été extraites avec PyMuPDF. Le manifeste donne pour chaque séance le lien, le SHA-256, la taille, le nombre de pages et le volume de texte extrait. Les figures, diagrammes et la mise en page n’ont pas été inspectés visuellement; les informations uniquement graphiques peuvent donc manquer. Les neuf séries d’exercices, quatre problem sets et l’examen final reliés par l’index n’ont pas été consultés.

## Contenu produit

Les 20 séances sont couvertes par des leçons françaises originales, avec objectifs, explications et exercices corrigés munis d’indices. Les exemples et questions ont été créés pour ce parcours; aucune banque d’exercices source n’est reproduite. Les thèmes suivent les notes : mécanismes (1–10), inefficacité des équilibres et routage (11–16), apprentissage et complexité de recherche d’équilibres (17–20).

Le cours est intégré dans `curriculum/courses/roughgarden_algorithmic_game_theory_2013.json` : 20 leçons et 41 exercices originaux. La revue indépendante a contrôlé le schéma et ciblé les enchères/Myerson/VCG, le routage, les liens regret–CCE/CE et la complexité PLS/PPAD. Les corrigés qui répétaient simplement la réponse ont été enrichis de leur raisonnement. Les identifiants manquants et un doublon d'options ont été corrigés avant intégration.

Une confusion de séance a été corrigée après comparaison directe des notes l11 et l12 : la séance 11 porte sur le routage non atomique affine et sa borne 4/3; la séance 12 aborde aussi le modèle atomique et sa borne 5/2. L'ancien exemple de ratio 15/10 ne convenait pas aux hypothèses non atomiques affines; il a été remplacé par le calcul de Pigou 1/(3/4). Un troisième exercice en séance 12 contrôle le cas atomique 10/4. Le parent a lu les passages correspondants des PDF officiels et confirmé les théorèmes et leur attribution.

`python tools/check_algorithmic_game_theory.py` appelle aussi `validate_course`, vérifie les 20 correspondances et les IDs, recalcule 29 réponses et contrôle 12 assertions conceptuelles. Les assertions conceptuelles de type `identity` reprennent une clé éditoriale fournie : elles ne sont pas des preuves indépendantes. Le checker distingue les deux exemples de routage et refuse les corrections identiques à l'option choisie. Le contrôle des textes, figures, problèmes ouverts et preuves générales ne se réduit pas à ces calculs finis. Le cours guide la pratique de chaque séance, sans remplacer les devoirs, séries d'exercices ni l'examen final originaux.
