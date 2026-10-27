# Plan : évolution vers l'Atelier IA

Date : 2026-10-26. Choix utilisateur : conserver l'app actuelle. Conception : [nouvelle direction](../docs/product/2026-10-26-learning-studio.md). Suivi : [todo.md](todo.md).

## Ordre de réalisation

1. Définir les contrats de séances, messages, configuration non secrète, stockage et tutorat. Ne pas modifier les données de progression existantes.
2. Livrer un contrôleur testable : créer, ouvrir, envoyer, annuler, reprendre, signaler les erreurs sans dupliquer les échanges ni perdre le brouillon.
3. Brancher le stockage Windows atomique et un vrai client HTTP compatible Chat Completions, sans nouvelle dépendance. Tests réseau contre serveur local; aucun appel payant pendant les tests.
4. Intégrer Atelier IA et la configuration du fournisseur dans la navigation Compose; conserver Cours, Profil et Paramètres. Tester fenêtres compacte et bureau.
5. Vérifier contrats, sauvegarde/reprise, erreurs, secrets et réseau; exécuter les régressions desktop puis construire la distribution et inspecter les captures natives.
6. Mettre à jour les documents et publier le code sur le dépôt. Ne pas confondre commit, artefact local et release distribuée.

## Après cette tranche

L'arbre de plan, le graphe de connaissances et la pratique adaptative dépendent d'abord d'une séance fiable et persistante. Ils sont suivis comme travaux futurs, sans marquer ces fonctionnalités comme implémentées par le premier écran de conversation.

## Critères de sortie

- Un utilisateur peut configurer un fournisseur, créer son objectif, recevoir une vraie réponse et rouvrir sa séance.
- Les tests automatisés n'utilisent aucune clé réelle ni service distant.
- Les secrets ne sont ni sauvegardés ni exposés par les messages d'erreur.
- Les échecs de lecture ne provoquent pas l'écrasement de la sauvegarde existante.
- Les cours et leur progression restent disponibles.
- Le résultat des tests, du build et de la publication est consigné séparément.
