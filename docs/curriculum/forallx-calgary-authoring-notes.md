# forall x: Calgary — notes de rédaction et de provenance

## Édition et portée

- Source : [forall x: Calgary, HTML](https://forallx.openlogicproject.org/html/)
- Édition affichée par la page de titre : Fall 2025, révision `c5cb415`, datée du 2026-09-27.
- Auteurs : P. D. Magnus, Tim Button, Robert Trueman et Richard Zach; contributions de J. Robert Loftis et Aaron Thomas-Bolduc.
- Licence du manifeste : CC BY 4.0. Les explications et exercices du brouillon sont rédigés en français; ils ne reproduisent pas les exercices de la source et ne suggèrent aucune approbation des auteurs.
- Les 48 unités suivent les 48 chapitres numérotés du livre. Ce ne sont pas des séances officielles. Appendices et solutions sélectionnées sont hors périmètre.

## Rédaction et revue

`curriculum/courses/forallx_calgary_2025.json` contient 48 leçons, une par chapitre, et 98 exercices à choix avec réponse, explication et indice. Une revue indépendante a parcouru les 98 questions/clés et comparé directement les chapitres 23–28, 36–39, 41–44 et 48 aux sources officielles : quantification, identité/substitution, règles de preuve, modalité et correction. La consigne s036_e03 a été reformulée pour demander la convention du manuel sur les domaines non vides, sans présupposer l'autorisation des domaines vides. La note s023 distingue maintenant le prédicat D de la formule D(x) ; l'expression « à compléter » déclenchait à tort le filtre anti-placeholder et a été reformulée sans affaiblir le validateur.

Les pages HTML officielles des chapitres 1 à 48 ont été consultées. Les notes pédagogiques suivent leur exposé et ciblent le contenu propre aux chapitres : arguments, notions sémantiques, TFL, preuves, FOL et quantification multiple, identité, descriptions définies, interprétations finies et propriétés des relations, règles quantificatrices, logique modale et cadres de Kripke, formes normales, complétude fonctionnelle, équivalences et correction. Les exemples et questions sont originaux. L’ordre, le titre source, l’URL directe et le SHA-256 de chaque chapitre sont conservés dans `forallx-calgary-source-sessions.json`.

## Vérifications et limites de preuve

`python tools/check_formal_logic.py` contrôle l’édition et les 48 correspondances chapitre/leçon, les URL directes, les empreintes au format SHA-256, le minimum de texte pédagogique, les exercices et leurs identifiants/réponses attendues. Il exécute aussi des vérifications bornées sur tables TFL, quantificateurs en domaines finis non vides, ordre des quantificateurs, identité et cardinalité, descriptions définies, relations finies, négation des quantificateurs et une petite structure modale. Ces calculs confirment les cas encodés et certaines clés de réponse; ils ne constituent pas une preuve générale de la logique du premier ordre, ne valident pas chaque phrase des leçons et ne remplacent pas une revue humaine des formulations et du corrigé.

Le manifeste enregistre la méthode et les limites de lecture : l’exposé HTML et le texte alternatif MathML ont été consultés, les tableaux de preuve ont été linéarisés, les diagrammes source n’ont pas été inspectés visuellement et les banques d’exercices/appendices ne sont pas revendiqués comme exhaustivement examinés. Les empreintes consignées sont des identifiants de relevé, pas une preuve d’archivage immuable ni de contrôle juridique de licence.

Le parent a exécuté `validate_course` sur le cours et son manifeste : 48 correspondances sans erreur après ces corrections. Aucun changement au validateur ou à son schéma n'a été nécessaire. Les tests et builds de l'application sont consignés séparément dans le registre d'exécution ; la revue de ces questions ne constitue pas une réalisation des problèmes ouverts, preuves détaillées ou exercices complets du manuel.
