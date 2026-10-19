# Duolingo KMP - App d'Apprentissage Multiplateforme (Kotlin / Compose Multiplatform)

Une application d'apprentissage style **Duolingo** développée en **Kotlin Multiplatform (KMP)** avec **Compose Multiplatform**, ciblant **Android**, **iOS (iPad / iPhone)** et **Windows Desktop**.

---

## 🎯 Domaines d'apprentissage inclus

1. **📐 Mathématiques Avancées** :
   - Algèbre linéaire, Vecteurs, Matrices, Produits scalaires & Valeurs propres.
   - Probabilités conditionnelles, Statistiques & Théorème de Bayes.

2. **💻 Informatique & Programmation C++** :
   - Structures de données (Arbres, Graphes, File/FIFO, Pile/LIFO).
   - Conception d'algorithmes et complexité temporelle $O(n \log n)$.
   - C++ moderne : Pointeurs, Déréférencement, Smart Pointers (`unique_ptr`, `shared_ptr`) & RAII.

3. **🇨🇦 Langues (Préparation TCF Canada & Anglais)** :
   - **TCF Canada** : Subjonctif présent, concordance des temps, compréhension écrite/orale & structures formelles.
   - **Anglais technique** : Vocabulaire de programmation et tournures professionnelles.

---

## Import de ressources (mode démonstration)

Le parcours accepte une référence YouTube ou un chemin PDF, mais ne lit aucun fichier et ne récupère aucune transcription. Aucun fournisseur IA ni service backend ne sont configurés. Les exercices créés sont des exemples associés à la matière sélectionnée, sans analyse de la ressource.

La progression et les leçons ajoutées restent en mémoire pendant la session. Elles sont perdues à la fermeture. La synchronisation GitHub n’est pas implémentée.

---
## ⚡ Comment tester et exécuter l'application ?

### Option 1 : Via Android Studio / IntelliJ IDEA (Recommandé)
1. Ouvrez **Android Studio** ou **IntelliJ IDEA**.
2. Choisissez **Open** et sélectionnez le dossier du projet : `D:\Coding\duolingo-kmp-learning-app`.
3. Pour exécuter la version **Windows Desktop** :
   - Dans le panneau Gradle (à droite), développez `composeApp -> Tasks -> compose desktop -> desktopRun`.
   - Cliquez deux fois sur **desktopRun** pour lancer la fenêtre de l'application sur Windows.
4. Pour exécuter la version **Android** :
   - Sélectionnez la configuration `composeApp` et lancez un émulateur Android ou connectez votre smartphone/iPad.

### Option 2 : Via la ligne de commande (Terminal)
Le dépôt ne contient pas encore de wrapper Gradle. Après installation de Gradle compatible avec Android Gradle Plugin 8.2.2, exécutez cette commande à la racine :
```bash
gradle :composeApp:desktopRun
```

---

## 📱 Structure Multiplateforme

- **Windows Desktop** : `composeApp/src/desktopMain/kotlin/main.kt`
- **Android App** : `composeApp/src/androidMain/kotlin/com/duolingo/app/MainActivity.kt`
- **Common Code (UI & Logique)** : `composeApp/src/commonMain/kotlin/com/duolingo/app`
