# DocFlow Universal Office Suite & Bidirectional Image-Document Converter

This upgrade expands **DocFlow** into a complete, cross-format document workspace supporting **PDF, Word (`.docx`), Excel (`.xlsx`), and PowerPoint (`.pptx`)** opening and structured block editing, paired with universal **Pic-to-Doc** (converting photos or CameraX scans into any of the four formats) and **Doc-to-Pic** (exporting any document into individual page/slide images or a stitched tall image) inside the **Apple Crystal Glass UI**.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices incorporate your confirmed preferences from the clarification step:

- **Confirmed Decision 1 — Unified Structured Text & Table Block Editor**: All four formats (PDF, Word, Excel, and PowerPoint) open into a unified, format-aware **Structured Text & Table Block Editor**. Excel spreadsheets parse and edit as structured tabular row/column blocks (`| Col A | Col B |`) with live formula/summary previews, PowerPoint decks organize into structured Slide blocks (`[Slide 1: Title & Bullets]`), and Word/PDF documents use structured section and paragraph blocks.
- **Confirmed Decision 2 — Hybrid Pic-to-Doc Conversion (OCR Text + Embedded Image Layout)**: When converting one or more pictures (from CameraX or Photo Picker) into **PDF, Word, Excel, or PowerPoint**, DocFlow performs structured text/table extraction alongside embedding the enhanced visual scan into the target document layout, so the resulting document has both the high-clarity image and editable structured blocks.
- **Confirmed Decision 3 — Dual-Mode Doc-to-Pic Export (Multi-Page & Stitched Single Image)**: Every PDF, Word, Excel, or PowerPoint document can be converted into high-resolution pictures (`PNG` / `JPEG`) in two selectable modes: **Individual Page/Slide/Sheet Images** or a **Single Stitched Long Image** combining all pages/slides vertically for instant previewing, saving, and sharing.

---

## 1. Overview & Core Concept

- **What It Does**:
  - **Universal Opener & Parser**: Opens local and cloud-synced `.pdf`, `.docx`/`.doc`, `.xlsx`/`.xls`/`.csv`, and `.pptx`/`.ppt` files using native OpenXML (`ZipInputStream` + `XmlPullParser` for Word `word/document.xml`, Excel `xl/sharedStrings.xml` + `xl/worksheets/sheet1.xml`, and PowerPoint `ppt/slides/slide*.xml`) and `PdfRenderer`.
  - **Structured Text & Table Block Editor**: Lets users add, reorder, and edit structured text blocks, key-value/spreadsheet table rows, and presentation slide cards in real time, syncing changes across devices via Firestore and indexing them in the local Room FTS4 engine.
  - **Pic-to-Doc (Image to PDF / Word / Excel / PowerPoint)**: Converts captured CameraX scans or gallery photos into any chosen target format (`PDF`, `Word`, `Excel`, or `PowerPoint`), combining extracted OCR text/table blocks with the enhanced embedded image.
  - **Doc-to-Pic (PDF / Word / Excel / PowerPoint to Image)**: Renders any open document into crisp, high-resolution bitmap pages—supporting both individual page/slide exports and a vertically stitched full-document composite image—which can also be saved back into the workspace as an image asset or shared externally.
- **Target Audience / Persona**: Mobile professionals, students, and field teams who work across PDFs, Word contracts, Excel financial sheets, and PowerPoint decks and need instant format conversion between physical photos and digital office documents.
- **Key Value**: Eliminates format silos by providing a single Apple Crystal Glass workspace where any picture can become a Word, Excel, PowerPoint, or PDF file, and any document can be edited or exported back to high-resolution images offline or online.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Universal File Opener & Format Filter**: From the Dashboard, users tap **Open File** to pick any PDF, Word, Excel, or PowerPoint file, or filter the workspace using format chips (`All`, `PDF`, `Word`, `Excel`, `PowerPoint`, `Scanned`, `Pic Exports`).
  2. **Pic-to-Doc Studio**: Users tap **Pic to Doc (Scan / Photo)**, capture pages with CameraX or select images from the gallery, apply enhancement filters (`Magic Color`, `B&W Sharp`, `Grayscale`), pick the target output format (**PDF**, **Word `.docx`**, **Excel `.xlsx`**, or **PowerPoint `.pptx`**), review the extracted text/table blocks + embedded image preview, and save/sync.
  3. **Structured Block Viewer & Live Editor**: Opening any document displays its visual page/slide/table preview alongside the **Structured Block Editor** (Paragraph blocks for Word/PDF, Table Row/Cell editors for Excel, and Slide Title/Bullet cards for PowerPoint).
  4. **Doc-to-Pic Converter Modal**: Inside any document (or directly from a document card action), tapping **Doc to Pic** opens an interactive converter with a toggle between **Multi-Page Gallery (PNG/JPG per page/slide)** and **Stitched Tall Image (All pages combined)**, with instant zoom preview, local file export, and 1-tap "Save to DocFlow Workspace".
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Apple Crystal Glass ("Liquid Glass")**—frosted translucent surfaces, diagonal specular rim borders, luminous ambient background orbs, and the Glossy DocFlow brand logo.
  - *Color Palette & Format Accents*:
    - **PDF**: Crimson Crystal (`#DC2626`)
    - **Word (`.docx`)**: Sapphire Crystal (`#0066FF`)
    - **Excel (`.xlsx`)**: Emerald Sheet Crystal (`#059669`)
    - **PowerPoint (`.pptx`)**: Amber/Sunset Slide Crystal (`#EA580C`)
    - **Scanned / Pic-to-Doc**: Electric Violet Crystal (`#6200EA`)
  - *Typography & Hierarchy*: High-contrast headings for document titles and slide headers, monospaced tabular alignment for Excel grid previews, and scannable pill badges for Room FTS4 tags and Cloud/Local sync status.
- **Interactive Feedback & Motion**: Smooth animated transitions between Visual Preview, Structured Block Editor, and Doc-to-Pic Gallery modes, with real-time sync status badges (`Cloud Synced` vs. `Local Only • Offline`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Zero-Dependency OpenXML Parsing for `.docx`, `.xlsx`, and `.pptx`**
  - *Chosen Approach*: Parse Office OpenXML archives directly using streaming `ZipInputStream` and `XmlPullParser` (`word/document.xml` for Word, shared string table + worksheet cells `xl/worksheets/sheet*.xml` for Excel, and `ppt/slides/slide*.xml` `<a:t>` runs for PowerPoint).
  - *Why*: Avoids multi-megabyte Apache POI reflection overhead and method-count bloat on Android while delivering fast, reliable on-device parsing and structured block reconstruction offline.
- **Decision 2: High-Resolution Canvas Page & Slide Rasterizer for Doc-to-Pic**
  - *Chosen Approach*: Use Android's `PdfRenderer` for native PDF bitmaps combined with a custom high-DPI `Canvas` document/slide/sheet renderer for Word, Excel, and PowerPoint structured blocks, supporting both individual page bitmaps and a vertically stitched composite bitmap.
  - *Why*: Guarantees every format (PDF, Word, Excel, PowerPoint) can be converted into crisp, shareable PNG/JPEG images whether the document originated from an imported file, a CameraX scan, or live structured edits.
- **Decision 3: Security Rules & Room FTS4 Schema Synchronization**
  - *Chosen Approach*: Expand the allowed `fileType` set in Firestore security rules (`['pdf', 'docx', 'doc', 'xlsx', 'xls', 'pptx', 'ppt', 'csv', 'scanned', 'pic', 'txt']`), verify with emulator tests, deploy the updated rules, and index all new formats and table/slide previews in Room FTS4.
  - *Why*: Maintains zero-trust per-user security while allowing seamless real-time cloud sync for Excel, PowerPoint, and converted image documents.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                     Apple Crystal Glass Compose UI                         │
│                                                                            │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐  │
│  │   DashboardScreen    │  │  PicToDocScanner &   │  │ DocumentViewer & │  │
│  │ • Format Filter Bar  │  │  Conversion Studio   │  │ Block Editor     │  │
│  │   (PDF/Word/XLS/PPT) │  │ • CameraX + Gallery  │  │ • Paragraph/Row/ │  │
│  │ • Room FTS4 Search   │  │ • Target Format Pick │  │   Slide Blocks   │  │
│  │ • Sync Status Badges │  │   (PDF/DOCX/XLSX/PPT)│  │ • Doc-to-Pic     │  │
│  └──────────┬───────────┘  └──────────┬───────────┘  └────────┬─────────┘  │
└─────────────┼─────────────────────────┼───────────────────────┼────────────┘
              │                         │                       │
              ▼                         ▼                       ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                        DocFlowViewModel (StateFlow)                        │
│  • Universal Document & Folder Streams    • Pic-to-Doc Format Generator    │
│  • Structured Block Serializer/Parser     • Doc-to-Pic Multi/Stitched Gen  │
└──────────────────┬──────────────────────────────────────┬──────────────────┘
                   │                                      │
                   ▼                                      ▼
┌─────────────────────────────────────┐  ┌───────────────────────────────────┐
│   Local Persistence & Media Engine  │  │     Cloud Persistence (Firebase)  │
│  • Room Database + FTS4 Virtual Tbl │  │  • Firestore Named DB             │
│  • OpenXML Parser (DOCX/XLSX/PPTX)  │  │  • Hardened Security Rules        │
│  • DocToPicRenderer (Multi & Stitch)│  │    (PDF/Word/Excel/PPTX/Pic)      │
│  • AES-256-CBC Crypto Engine        │  │  • CredentialManager Google Auth  │
└─────────────────────────────────────┘  └───────────────────────────────────┘
```

### Data Model & State

- **Supported `fileType` Values**: `"pdf"`, `"docx"`, `"xlsx"`, `"pptx"`, `"scanned"`, `"pic"`, `"txt"`.
- **Structured Block Representation**:
  - Documents store structured blocks (Paragraphs, Spreadsheet Table Rows `| Cell 1 | Cell 2 |`, or Presentation Slides `=== SLIDE N: Title ===`) alongside optional embedded image paths (`localFilePath`) so both visual layout and structured text/table editing work seamlessly across devices.
- **Doc-to-Pic Output Modes**:
  - `ExportMode.MULTI_PAGE`: Generates a list of `Bitmap` pages/slides/sheets and writes individual `.png` files to local storage.
  - `ExportMode.STITCHED_SINGLE`: Vertically composites all rendered pages/slides into a single high-resolution tall `.png` image with subtle page dividers.

### Interactive Component & State Mapping

- **Dashboard Universal Picker & Filters**: Launches `ActivityResultContracts.OpenDocument` configured with MIME types for PDF, Word (`.docx`/`.doc`), Excel (`.xlsx`/`.xls`/`.csv`), PowerPoint (`.pptx`/`.ppt`), and images (`image/*` for direct Pic-to-Doc), and filters the list by format, category, folder, or Room FTS4 tag query.
- **Pic-to-Doc Conversion Flow**: Captures or imports 1+ images -> applies selected color matrix filter -> user selects target format (`PDF`, `Word`, `Excel`, or `PowerPoint`) -> generates structured OCR/table/slide blocks + embedded image PDF/container -> saves to Room FTS4 & Firestore.
- **Structured Block Editor & Doc-to-Pic Flow**: Parses document content into interactive blocks (add/edit/remove table rows for Excel, slide cards for PowerPoint, or sections for Word/PDF) -> saves live edits to Room & Firestore -> **Doc to Pic** button renders the current document into either multi-page PNGs or a single stitched tall PNG with immediate preview and workspace save.
