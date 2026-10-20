# Atelier original de lecture — trois ateliers, douze questions

Rédaction et consultation des références : 20 octobre 2026.

## Périmètre et provenance

Cette unité comprend trois ateliers locaux et quatre documents fictifs : un message de collecte de livres, une annonce de cycle photographique, puis deux contributions sur une rue scolaire. Les auteurs, lieux, tarifs, horaires et projets de ces documents sont inventés pour les exercices. Ce sont des documents complets rédigés pour être lus, et non des résumés de cours présentés comme une épreuve de compréhension. Les textes, questions, distracteurs, indices et corrigés ont été composés pour cette unité ; aucun item d’une banque FEI, TV5MONDE ou RFI n’est intégré.

Le manifeste porte `sequenceType: original_practice_formats`, trois sessions et `kind: practice`. Cette progression est un choix pédagogique local : elle ne reproduit aucun programme de cours FEI. Les douze questions ne constituent ni une épreuve officielle, ni une simulation intégrale, ni une préparation complète au TCF Canada. Aucun niveau CECRL/NCLC ni score officiel n’est déduit des réponses ou des XP de l’application. Les répétitions et les essais corrigés relèvent de l’entraînement.

## Références consultées et recommandations

- [FEI — TCF Canada](https://www.france-education-international.fr/test/tcf-canada) : la description officielle indexée présente une compréhension écrite sous forme de QCM à quatre réponses, dont une seule correcte. L’ouverture directe de cette page a retourné ERR-BOT-403 ; la description indexée a été consultée, sans prétendre avoir lu la page complète.
- [FEI — exemples d’épreuves](https://www.france-education-international.fr/test/exemples-epreuves-tcf) : lien recommandé pour découvrir les supports officiels. Son ouverture directe a également retourné ERR-BOT-403. Aucun exemple n’a été reproduit.
- [TV5MONDE — entraînement au TCF](https://apprendre.tv5monde.com/fr/tcf) et [présentation des livrets](https://apprendre.tv5monde.com/fr/article/les-livrets-dentrainement-au-tcf-r-avec-tv5monde) : pages ouvertes pour vérifier la présence d’une offre d’entraînement à la compréhension écrite. Elles sont proposées comme ressources complémentaires externes, sans importation de leurs items. Les entraînements TV5MONDE ont leurs propres conditions ; cette unité ne revendique ni leur validation pédagogique ni leur équivalence avec un résultat d’examen.
- [Le français facile avec RFI](https://francaisfacile.rfi.fr/fr/) : ressource complémentaire pour travailler la langue et utiliser des contenus avec transcription. L’ouverture du portail est bloquée par robots.txt dans cet environnement. La [brochure officielle du service](https://s.francaisfacile.rfi.fr/media/display/492a1bbe-2e52-11ef-8329-005056a90284/RFI_fran%C3%A7ais_facile_BROCHURE_210x210-HD-PourAffichageEcran.pdf), consultée par son texte indexé, décrit notamment l’entraînement à la compréhension orale. RFI n’est donc pas présenté ici comme la source d’une banque de compréhension écrite TCF Canada.

Les pages externes s’ouvrent uniquement à la demande de l’utilisateur. Les documents originaux de l’unité restent dans le JSON ; aucune connexion n’est nécessaire pour les lire une fois l’unité intégrée à l’application.

## Choix d’affichage

Le modèle `Exercise.ReadingChoice` distingue le `passage` du `prompt`, qui ne contient que la question. Les douze exercices utilisent le type JSON `reading`. Chaque passage est reproduit intégralement pour sa question : rien n’exige de mémoriser le texte d’une question précédente ou de revenir aux notes pour trouver une information. Les notes décrivent une démarche générale et ne contiennent pas le corrigé du document. L’atelier à deux opinions fournit les deux contributions à chacune de ses quatre questions.

La question conserve le style du titre d’exercice ; le document est présenté séparément dans une surface blanche, en caractères normaux de 18 sp avec un interligne de 28 sp. Le texte peut être sélectionné et utilise le défilement de l’écran, sans zone de défilement imbriquée. Les quatre réponses puis la correction restent dans ce même flux. L’inspection visuelle de ce contenu dans l’application n’a pas été effectuée.

## Contrôle éditorial

| Atelier | Questions et preuves de la bonne réponse |
| --- | --- |
| Message pratique | e1 : modification explicite de la plage horaire ; e2 : maintien des conditions de don malgré l’exception d’inscription ; e3 : démarche conditionnelle pour l’après-midi ; e4 : intention générale d’organisation. |
| Annonce détaillée | e1 : débutants admis et téléphone suffisant ; e2 : tarif du cycle sans impression incluse ; e3 : rendez-vous et matériel précisés par courriel ; e4 : autorisation d’exposition distincte de la participation. |
| Comparaison d’arguments | e1 : acceptation d’un essai par les deux auteurs ; e2 : report possible des difficultés vers la rue parallèle ; e3 : bilan devant inclure les voies voisines ; e4 : accord conditionnel plutôt que refus ou adhésion absolue. |

Chaque question propose quatre options et une seule réponse justifiée. Les distracteurs changent un horaire, un public, une condition, le périmètre d’une règle ou le degré d’adhésion ; ils ne nécessitent pas de connaissance d’un fait extérieur. Les positions correctes sont réparties de façon équilibrée : trois fois chacune aux indices 0, 1, 2 et 3. Les notes comptent respectivement 122, 120 et 125 mots selon un comptage par espaces.

Le validateur exige un passage séparé non vide ; le générateur produit `Exercise.ReadingChoice` avec ce passage. Un test Python rejette les passages absents ou invalides et un autre vérifie la génération séparée du passage et de la question. Un garde de régression vérifie qu’une erreur de couverture empêche toute génération, même avec `--generate` : un brouillon incomplet ne devient pas un cours publié. Le test Kotlin de parcours de tous les contenus comprend désormais la branche lecture. Aucun build natif ou commit n’est exécuté par cette tâche ; les résultats de validation et la régénération sont rapportés à la tâche principale, qui exécute séparément son build. La revue dans le lecteur natif reste nécessaire.
