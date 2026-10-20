# Northeastern CS3500 Fall 2019 — notes d’auteur

## État

Le calendrier officiel liste 25 réunions de cours, y compris les deux examens, et indique explicitement que Thanksgiving (29 novembre) est sans cours. Les neuf devoirs apparaissent dans une section séparée et ne sont pas comptés comme séances. Le cours annoncé JavaScript du 26 novembre est le seul élément de la séquence sans leçon rédigée : le calendrier ne fournit aucun support de séance, et la recherche ciblée `site:course.ccs.neu.edu/cs3500f19 JavaScript` ne révèle que la page du cours. Le brouillon contient donc 24 leçons sur 25 et reste hors catalogue.

## Lecture des supports

Les pages HTML officielles de notes ont été récupérées et leur texte extrait en mémoire pour lecture ; elles n’ont pas été archivées localement. Les empreintes SHA-256 indiquées dans le manifeste identifient les octets consultés. Les thèmes développés comprennent l’introduction à l’OOD, objets et publications, revue Java, safari Java, modèle/Builder, contrôleurs et mocks, invariants, Puralax, héritage/composition, performance I et II, GUI, adaptateur et stratégies/décorateur. La page officielle de testing et le Master List de principes ont aussi été lus.

Le 20 septembre ne possède pas de notes propres : la leçon s’appuie sur les notes Safari et modèle disponibles, sans prétendre reproduire une transcription. Les séances de révision et les activités sans notes sont des applications originales de documents déjà lus. Les jours d’examen ne disposent que de la mention « In class » ; leurs exercices sont des entraînements originaux, non des questions d’examen. Le 15 novembre, le calendrier lie une archive `code.zip` ; son type ZIP a été refusé par l’outil de lecture, donc le contenu SceneGraph n’est pas revendiqué. La leçon correspondante s’appuie seulement sur la page des principes de conception. Le wrap-up n’a pas de notes dédiées et est explicitement une synthèse originale.

## Contrôle pédagogique

Chaque leçon rédigée contient une explication française d’au moins 40 mots, des exercices originaux, des indices et des raisonnements de correction appliqués aux données de la question. Les IDs d’exercice sont stables et vérifiés pour unicité. Le checker appelle le validateur partagé sans générer de données. Il renvoie `PARTIAL` et le code de sortie 2 tant que la séance JavaScript n’a pas de source exploitable ; aucun contenu n’est inféré de son seul titre.

## Sources officielles

- [Calendrier et syllabus Fall 2019](https://course.ccs.neu.edu/cs3500f19/)
- [Notes officielles sur le testing](https://course.ccs.neu.edu/cs3500f19/testing.html)
- [Master List des principes de conception](https://course.ccs.neu.edu/cs3500f19/design-principles.html)

La liste exacte des réunions, les liens de supports par séance, les empreintes des notes effectivement consultées et les limites de couverture figurent dans `neu-ood-source-sessions.json`.
