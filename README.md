# NotesApp (Užrašai)

**NotesApp** – tai „Android“ užrašų programėlė, sukurta pagal **MIPT praktinio darbo #4 („Activity elementų sąveika ir duomenų saugojimas“)** reikalavimus. Programėlė skirta demonstruoti kelių `Activity` ekranų tarpusavio sąveiką, duomenų perdavimą per `Intent` ir `ActivityResultLauncher` mechanizmą bei dviejų skirtingų duomenų saugojimo būdų (**SQLite** duomenų bazės ir **JSON** failo) naudojimą su galimybe juos sinchronizuoti.

---

## Turinys

1. [Projekto aprašymas ir paskirtis](#1-projekto-aprašymas-ir-paskirtis)
2. [Technologijos ir reikalavimai](#2-technologijos-ir-reikalavimai)
3. [Funkcionalumo apžvalga ir vartotojo instrukcija](#3-funkcionalumo-apžvalga-ir-vartotojo-instrukcija)
4. [Projekto architektūra ir komponentų aprašymas](#4-projekto-architektūra-ir-komponentų-aprašymas)
5. [Lokalizavimo palaikymas (EN / LT)](#5-lokalizavimo-palaikymas-en--lt)
6. [Code Review ir įgyvendintų patobulinimų suvestinė](#6-code-review-ir-įgyvendintų-patobulinimų-suvestinė)
7. [Projekto paleidimas](#7-projekto-paleidimas)

---

## 1. Projekto aprašymas ir paskirtis

### Praktinio darbo tikslas
Šio darbo pagrindinis tikslas – praktiškai įgyvendinti **„Android“ komponentų (`Activity`) sąveiką** ir **nuolatinį duomenų saugojimą** mobiliojoje aplikacijoje:
- Naviguoti tarp kelių ekranų (`MainActivity`, `NoteDetailActivity`, `AddEditNoteActivity`), perduodant kontekstinius duomenis (`note_id`, `source`, `mode`, `title`, `content`) per `Intent`.
- Apdoroti grįžtančius rezultatus naudojant modernų `ActivityResultLauncher<Intent>` (`ActivityResultContracts.StartActivityForResult`) mechanizmą.
- Realizuoti vieningą duomenų prieigos sąsają (`NoteStorage`), leidžiančią tą pačią verslo logiką naudoti tiek su **SQLite** reliacine duomenų baze, tiek su vidinėje atmintyje laikomu **JSON** failu.
- Išsaugoti vartotojo pasirinktą duomenų šaltinį tarp programėlės sesijų naudojant `SharedPreferences`.

---

## 2. Technologijos ir reikalavimai

| Parametras | Reikšmė |
| :--- | :--- |
| **Programavimo kalba** | Java (Java 11) |
| **Application ID** | `com.example.notesapp` |
| **Minimali SDK versija (`minSdk`)** | 24 (Android 7.0) |
| **Kompiliavimo / tikslinė SDK (`compileSdk` / `targetSdk`)** | 37 |
| **Android Gradle Plugin (AGP)** | 9.4.1 |
| **UI biblioteka** | Material Components for Android (`material:1.14.0`, Material 3), `AppCompat`, `RecyclerView`, `CoordinatorLayout` |
| **Sąsajos pritaikymas** | Edge-to-Edge (`WindowCompat.setDecorFitsSystemWindows(..., false)` ir `WindowInsetsCompat`) |

---

## 3. Funkcionalumo apžvalga ir vartotojo instrukcija

Programėlė palaiko visas pagrindines užrašų valdymo operacijas (CRUD – kūrimą, skaitymą, atnaujinimą, trynimą), duomenų šaltinio perjungimą bei sinchronizavimą.

### 3.1. Užrašo sukūrimas
1. Pagrindiniame ekrane (`MainActivity`) paspauskite apatiniame dešiniajame kampe esantį plaukiojantį mygtuką **`+`** (`FloatingActionButton`).
2. Atsidarys kūrimo ekranas (**„Naujas Užrašas“** / **„Add Note“**).
3. Įveskite užrašo **Pavadinimą** (`Title`) ir **Turinį** (`Content`).
4. Viršutinėje įrankių juostoje paspauskite išsaugojimo piktogramą (**„Išsaugoti“** / **„Save“**).
   - *Validacija:* abu laukai yra privalomi. Jei bent vienas laukas tuščias (įvertinus tarpus pradžioje ir pabaigoje), ties atitinkamu lauku parodomas klaidos pranešimas ir rodomas bendras įspėjimas.
5. Sėkmingai išsaugojus, užrašas įrašomas į **šiuo metu aktyvų saugojimo šaltinį** (SQLite arba JSON failą), parodomas patvirtinimo pranešimas, o pagrindinio ekrano sąrašas automatiškai atnaujinamas.

### 3.2. Užrašo peržiūra
1. Pagrindiniame ekrane esančiame sąraše rodomos visų pasirinkto šaltinio užrašų kortelės (pavadinimas ir iki 2 eilučių turinio peržiūra).
2. Paspauskite ant bet kurios užrašo kortelės – atsidarys peržiūros ekranas (**„Užrašo Peržiūra“** / **„Note Detail“**), kuriame matomas visas pavadinimas ir slenkamas pilnas užrašo tekstas.
3. Norėdami grįžti atgal į sąrašą, paspauskite įrankių juostos rodyklę atgal arba sistemos grįžimo mygtuką.

### 3.3. Užrašo redagavimas
1. Atidarykite užrašą peržiūros ekrane (`NoteDetailActivity`).
2. Apatinėje ekrano dalyje paspauskite mygtuką **„Redaguoti“** (**„Edit“**).
3. Atsidarys redagavimo ekranas (**„Redaguoti Užrašą“** / **„Edit Note“**), kuriame jau bus užpildyti esami užrašo duomenys.
4. Atlikite norimus pakeitimus ir paspauskite įrankių juostos išsaugojimo mygtuką.
5. Išsaugojus duomenys atnaujinami aktyviame šaltinyje, atnaujinamas peržiūros ekranas, o grįžus į pagrindinį ekraną atnaujinamas ir bendras užrašų sąrašas.

### 3.4. Užrašo ištrynimas
1. Atidarykite užrašą peržiūros ekrane (`NoteDetailActivity`).
2. Apatinėje ekrano dalyje paspauskite mygtuką **„Ištrinti“** (**„Delete“**).
3. Pasirodžiusiame patvirtinimo dialogo lange (**„Ar tikrai norite ištrinti šį užrašą?“**) pasirinkite **„Taip“** (**„Yes“**), jei norite patvirtinti veiksmą, arba **„Ne“** (**„No“**), jei norite atšaukti.
4. Patvirtinus, užrašas pašalinamas iš aktyvaus šaltinio ir vartotojas grąžinamas į atnaujintą pagrindinį sąrašą.

### 3.5. Saugojimo šaltinio perjungimas (SQLite ↔ JSON failas)
1. Pagrindinio ekrano viršuje, po įrankių juosta, yra šaltinio pasirinkimo juosta su dviem parinktimis:
   - **SQLite** – duomenys saugomi vietinėje SQLite duomenų bazėje (`notes.db`).
   - **JSON Failas** (**JSON File**) – duomenys saugomi vidiniame programėlės faile `notes_secondary.json`.
2. Pasirinkus kitą radijo mygtuką, sąrašas akimirksniu persikrauna iš naujai pasirinkto šaltinio.
3. Pasirinkimas automatiškai įrašomas į `SharedPreferences` (`NotesAppPrefs`, raktas `selected_source`), todėl išlieka net visiškai uždarius ir iš naujo paleidus programėlę.

### 3.6. Kaip veikia sinchronizavimas
1. Pagrindiniame ekrane šalia šaltinio pasirinkimo mygtukų yra mygtukas **„Sinchronizuoti“** (**„Sync“**).
2. Paspaudus šį mygtuką, programa nuskaito visus užrašus iš **SQLite** duomenų bazės:
   - Jei SQLite duomenų bazėje užrašų nėra, parodomas pranešimas *„Nėra užrašų sinchronizuoti“*, o JSON failas paliekamas nepakeistas.
   - Jei SQLite duomenų bazėje yra bent vienas užrašas, visas `notes_secondary.json` failo turinys **vienu kryptingu veiksmu (SQLite → JSON)** perrašomas aktualiais SQLite duomenimis (`overwriteAll`).
3. Sėkmingai sinchronizavus, parodomas pranešimas su sinchronizuotų užrašų skaičiumi, o jei tuo metu peržiūrimas JSON šaltinis – sąrašas iškart atnaujinamas.

---

## 4. Projekto architektūra ir komponentų aprašymas

Projektas suskirstytas į aiškius sluoksnius pagal atsakomybę: **UI (`Activity` ir `Adapter`)**, **duomenų modelį (`model`)**, **duomenų bazės pagalbines klases (`db`)** ir **saugojimo abstrakciją (`storage`)**.

### Paketų struktūra

```text
app/src/main/java/com/example/notesapp/
├── MainActivity.java                 # Pagrindinis ekranas: sąrašas, šaltinio jungiklis, Sync, FAB
├── NoteDetailActivity.java           # Užrašo peržiūra, redagavimo ir šalinimo veiksmai
├── AddEditNoteActivity.java          # Naujo užrašo kūrimo ir esamo redagavimo forma
├── adapter/
│   └── NotesAdapter.java             # RecyclerView adapteris užrašų kortelėms atvaizduoti
├── db/
│   └── NotesDatabaseHelper.java      # SQLiteOpenHelper (Singleton) DB schemai valdyti
├── model/
│   └── Note.java                     # Duomenų modelis (id, title, content)
└── storage/
    ├── NoteStorage.java              # Bendra CRUD sąsaja saugykloms
    ├── SqliteNoteStorage.java        # NoteStorage realizacija su SQLite
    └── JsonNoteStorage.java          # NoteStorage realizacija su JSON failu
```

### Komponentų sąveikos schema

```mermaid
flowchart TD
    MA["MainActivity\n(Sąrašas, RadioGroup, Sync, FAB)"] -->|Intent + EXTRA_NOTE_ID, EXTRA_SOURCE| NDA["NoteDetailActivity\n(Peržiūra, Redaguoti, Ištrinti)"]
    MA -->|Intent + MODE_ADD, EXTRA_SOURCE| AEA["AddEditNoteActivity\n(Pridėti / Redaguoti)"]
    NDA -->|Intent + MODE_EDIT, note data| AEA
    MA --> NA["NotesAdapter\n(RecyclerView)"]
    NA --> M["Note\n(Modelis)"]

    MA --> NS["«interface»\nNoteStorage"]
    NDA --> NS
    AEA --> NS

    NS <|.. SNS["SqliteNoteStorage"]
    NS <|.. JNS["JsonNoteStorage"]
    SNS --> DBH["NotesDatabaseHelper\n(notes.db)"]
    JNS --> JF["notes_secondary.json\n(Internal Storage)"]
```

### Pagrindinių klasių ir Activity aprašymas

- **`MainActivity`** (`com.example.notesapp.MainActivity`)  
  Pagrindinis aplikacijos paleidimo ekranas (`LAUNCHER`). Valdo `RecyclerView` sąrašą per `NotesAdapter`, saugojimo šaltinio pasirinkimą per `RadioGroup` ir `SharedPreferences` (`NotesAppPrefs`), vienkryptį sinchronizavimą iš SQLite į JSON (`performSync()`) bei navigaciją į `NoteDetailActivity` ir `AddEditNoteActivity` per `ActivityResultLauncher<Intent>`. Taip pat apibrėžia bendras `Intent` raktų ir režimų konstantas (`EXTRA_NOTE_ID`, `EXTRA_SOURCE`, `EXTRA_MODE`, `EXTRA_TITLE`, `EXTRA_CONTENT`, `MODE_ADD`, `MODE_EDIT`, `SOURCE_SQLITE`, `SOURCE_JSON`) ir pritaiko `WindowInsets` slankiojančiam mygtukui bei sąrašui.

- **`NoteDetailActivity`** (`com.example.notesapp.NoteDetailActivity`)  
  Skirtas pasirinkto užrašo pilnam turiniui peržiūrėti. Pagal per `Intent` gautus `note_id` ir `source` sukuria atitinkamą `NoteStorage` realizaciją ir užkrauna užrašą (`loadNote()`). Leidžia pereiti į redagavimą (`openEditNote()` per `editLauncher`) arba ištrinti užrašą patvirtinus veiksmą `AlertDialog` lange (`showDeleteConfirmDialog()`). Pakeitus ar ištrynus užrašą, nustato `RESULT_OK`, kad `MainActivity` atnaujintų sąrašą.

- **`AddEditNoteActivity`** (`com.example.notesapp.AddEditNoteActivity`)  
  Universalus formos ekranas tiek naujo užrašo sukūrimui (`MODE_ADD`), tiek esamo redagavimui (`MODE_EDIT`). Įrankių juostoje atvaizduoja išsaugojimo veiksmą (`menu_add_edit.xml` / `R.id.action_save`). Prieš įrašant duomenis atlieka tuščių laukų validaciją su `TextInputLayout.setError(...)`, o išsaugojimą vykdo per `NoteStorage` (`addNote` arba `updateNote`).

- **`NoteStorage`** (`com.example.notesapp.storage.NoteStorage`)  
  Sąsaja (*interface*), apibrėžianti nepriklausomą nuo konkrečios saugyklos duomenų prieigos kontraktą:
  - `long addNote(Note note)`
  - `boolean updateNote(Note note)`
  - `boolean deleteNote(long id)`
  - `List<Note> getAllNotes()`
  - `Note getNoteById(long id)`

- **`SqliteNoteStorage`** (`com.example.notesapp.storage.SqliteNoteStorage`)  
  `NoteStorage` realizacija, dirbanti su SQLite duomenų baze per `NotesDatabaseHelper.getInstance(context)`. Atlieka parametrines užklausas (`insert`, `update`, `delete`, `query`), rikiuoja sąrašą pagal `id ASC` ir apdoroja galimas duomenų bazės išimtis.

- **`JsonNoteStorage`** (`com.example.notesapp.storage.JsonNoteStorage`)  
  `NoteStorage` realizacija, sauganti užrašus JSON masyvo formatu vidiniame programėlės faile `notes_secondary.json` (`context.getFilesDir()`). Automatiškai generuoja unikalius ID (`maxId + 1`), realizuoja visus CRUD metodus ir papildomą metodą `overwriteAll(List<Note> notes)`, kurį naudoja `MainActivity` sinchronizavimo funkcija.

- **`NotesDatabaseHelper`** (`com.example.notesapp.db.NotesDatabaseHelper`)  
  `SQLiteOpenHelper` paveldėtojas, valdantis duomenų bazę `notes.db` (versija `1`) ir lentelę `notes` su stulpeliais `id` (`INTEGER PRIMARY KEY AUTOINCREMENT`), `title` (`TEXT NOT NULL`) bei `content` (`TEXT NOT NULL`). Naudoja gijų saugų `getInstance(Context)` Singleton modelį.

- **`NotesAdapter`** (`com.example.notesapp.adapter.NotesAdapter`)  
  `RecyclerView.Adapter` realizacija su `NoteViewHolder` ir paspaudimo klausytojo sąsaja `OnNoteClickListener`. Atvaizduoja užrašus `item_note.xml` kortelėse ir atnaujina sąrašą metodu `updateNotes(List<Note>)`.

- **`Note`** (`com.example.notesapp.model.Note`)  
  Paprastas POJO duomenų modelis, turintis laukus `id` (`long`), `title` (`String`) ir `content` (`String`), konstruktorius bei atitinkamus `get`/`set` metodus.

---

## 5. Lokalizavimo palaikymas (EN / LT)

Programėlė palaiko dvi kalbas per standartinę „Android“ išteklių lokalizavimo sistemą:

| Kalba | Išteklių failas | Programėlės pavadinimas |
| :--- | :--- | :--- |
| **Anglų (numatytoji / EN)** | `app/src/main/res/values/strings.xml` | `Notes App` |
| **Lietuvių (LT)** | `app/src/main/res/values-lt/strings.xml` | `Užrašai` |

Išverstos visos naudotojo sąsajos eilutės:
- Ekranų pavadinimai (`title_note_detail`, `title_add_note`, `title_edit_note`)
- Šaltinių ir mygtukų užrašai (`source_sqlite`, `source_json`, `btn_sync`, `btn_save`, `btn_edit`, `btn_delete`)
- Formos laukų užuominos ir validacijos klaidos (`hint_title`, `hint_content`, `error_empty_title`, `error_empty_content`)
- Patvirtinimo dialogo tekstai (`dialog_delete_title`, `dialog_delete_message`, `dialog_yes`, `dialog_no`)
- Pranešimai (`Toast`), įskaitant parametrizuotą sinchronizavimo pranešimą (`toast_sync_success` su `%d` žymekliu)

Kalba parenkama automatiškai pagal „Android“ įrenginio sistemos kalbos nustatymus.

---

## 6. Code Review ir įgyvendintų patobulinimų suvestinė

Atlikus kodo peržiūrą (*Code Review*), buvo išskirtos ir įgyvendintos **2 aukščiausio prioriteto patobulinimų sritys**, tiesiogiai didinančios duomenų vientisumą ir programėlės stabilumą.

### 1. Išteklių valdymas ir atominis JSON rašymas
- **Singleton DB pagalbininkas (`NotesDatabaseHelper`):** įdiegtas `synchronized getInstance(Context)` su `context.getApplicationContext()`, todėl išvengiama `Activity` konteksto nutekėjimo (*memory leak*) ir užtikrinamas saugus vienos duomenų bazės jungties valdymas.
- **Automatinis kursorių ir srautų uždarymas:** `SqliteNoteStorage` metoduose (`getAllNotes`, `getNoteById`) visi `Cursor` objektai, o `JsonNoteStorage` – `BufferedReader` ir `FileWriter` srautai valdomi per `try-with-resources`, todėl ištekliai garantuotai uždaromi net įvykus išimčiai.
- **Gijų sinchronizavimas ir atominis JSON failo perrašymas (`JsonNoteStorage`):**
  - Visi skaitymo ir rašymo veiksmai sinchronizuojami per vidinį `lock` objektą (`synchronized (lock)`), apsaugant nuo duomenų lenktynių (*race conditions*).
  - Rašant į JSON failą (`writeAll`), duomenys pirmiausia įrašomi ir išvalomi (`flush()`) į laikiną failą `notes_secondary.json.tmp`, o tik sėkmingai užbaigus senasis failas pakeičiamas atominiu pervadinimu (`tempFile.renameTo(file)`). Klaidos atveju laikinas failas išvalomas, todėl pagrindinis `notes_secondary.json` failas nėra sugadinamas nutrūkus rašymo operacijai.

### 2. Ekrano pasukimo būsenos išsaugojimas ir klaidų valdymo pataisymai
- **Redagavimo būsenos išsaugojimas pasukus ekraną (`AddEditNoteActivity`):** redagavimo režime (`MODE_EDIT`) pradiniai laukai iš `Intent` užpildomi tik tada, kai `savedInstanceState == null`. Dėl to pasukus ekraną ar įvykus kitam konfigūracijos pasikeitimui, sistema išsaugo vartotojo jau įvestą, bet dar neišsaugotą tekstą, o ne perrašo jį pradinėmis reikšmėmis iš `Intent`.
- **Nuoseklus klaidų tikrinimas ir grįžtamasis ryšys vartotojui:**
  - `SqliteNoteStorage` ir `JsonNoteStorage` operacijos apsaugotos `try-catch` blokais, registruoja klaidas per `Log.e` ir grąžina aiškius statusus (`-1`, `false`, `null`).
  - `AddEditNoteActivity` uždaro ekraną ir grąžina `RESULT_OK` tik tuomet, kai išsaugojimas tikrai pavyko; nesėkmės atveju vartotojas lieka formoje ir mato klaidos pranešimą (`toast_operation_failed`), neprarasdamas įvesto teksto.
  - `NoteDetailActivity` tikrina ar `noteId != -1` bei ar užrašas rastas saugykloje; neradus užrašo, parodomas `toast_note_not_found` ir ekranas saugiai uždaromas. Taip pat tikrinamas trynimo rezultatas (`storage.deleteNote(noteId)`) ir užtikrintas patikimas įrankių juostos grįžimo mygtuko veikimas tiek per `onOptionsItemSelected` / `onSupportNavigateUp`, tiek per `toolbar.setNavigationOnClickListener`.

---

## 7. Projekto paleidimas

1. Atidarykite projektą `NotesApp` naudojant **Android Studio**.
2. Palaukite, kol **Gradle** sinchronizuos projekto priklausomybes.
3. Pasirinkite „Android“ emuliatorių arba fizinį įrenginį (su Android 7.0 / API 24 arba naujesne versija).
4. Paleiskite programėlę paspausdami **Run (`app`)** arba komandinėje eilutėje įvykdykite:
   ```bash
   ./gradlew assembleDebug
   ```
