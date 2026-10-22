# DocFlow: Live Collaborative Editing, Recent Files & Storage Dashboard Plan

An upgrade to DocFlow introducing Google-Docs-style real-time collaborative editing with end-to-end encryption, an expandable Room-prioritized Recent Files drawer, and a dedicated Visual Storage Dashboard with an interactive Donut Chart.

> [!NOTE]
> ### User Review & Critical Decisions
> Based on your input, the plan incorporates the following confirmed product and design decisions:
> - **Storage Dashboard Placement**: Added as a dedicated **Storage** tab in the main workspace navigation, featuring an interactive Donut Chart where tapping any document format slice (`PDF`, `DOCX`, `PPTX`, `XLSX`, `PIC`) highlights its metrics and lets you filter your workspace directly by that format.
> - **Recent Files Presentation**: Positioned as an expandable **Recent Files** horizontal scroll drawer directly above the folder bar on the Home workspace, dynamically prioritized by Room `lastAccessedAtMillis` timestamps whenever documents are opened, created, or edited.
> - **Google-Docs-Style Live Editing Scope**: Includes full sub-second live keystroke/block autosave (eliminating manual save buttons and conflict copies), active collaborator presence and colored cursor indicators, inline threaded comments with `@mentions`, suggesting mode (track changes with accept/reject), and named version history snapshots with non-destructive restoration.

---

## 1. Overview & Core Concept
DocFlow will evolve into a unified, real-time collaborative document workspace while preserving its signature **Apple Crystal Glass** aesthetic and zero-trust client-side encryption:
- **Instant Access via Room Last-Access Priority**: Users can immediately resume work on recently viewed or edited documents through an expandable glassmorphic horizontal carousel at the top of the workspace.
- **Visual Storage Intelligence**: A dedicated Storage navigation tab gives users an immediate visual breakdown of their local and cloud storage footprint across `PDF`, `Word`, `PowerPoint`, `Excel`, and `Images/Scans`, paired with one-tap format filtering and offline cache controls.
- **Google-Docs-Style Live Collaboration**: Opening any Word or structured document transitions into a live, conflict-free collaborative session. Edits persist locally in Room within milliseconds and broadcast to collaborators with AES-256-GCM encryption, complete with live collaborator cursors, inline comments, tracked suggestions, and version snapshots.

---

## 2. User Experience & Visual Design
- **Aesthetic Direction & Color Palette**:
  - Continues the **Apple Liquid / Crystal Glass** UI system: translucent frosted surfaces (`85%`–`92%` alpha), `1dp` directional specular top-left borders, deep slate-violet dark mode (`#070A12`), and crisp alabaster light mode (`#F4F7FB`).
  - **Format & Collaborator Accents**: Electric Royal Blue (`#0066FF`) for Word/DOCX, Crimson Coral (`#E11D48`) for PDF, Amber Gold (`#D97706`) for PowerPoint/PPTX, Emerald (`#059669`) for Excel/XLSX, and Cyber Violet (`#5E17EB`) for Images/Scans.
- **Key Screens & Flows**:
  1. **Workspace Home & Expandable Recent Files Drawer**:
     - Positioned directly above the folder selector bar.
     - Displays a collapsible header with a count badge and quick-toggle chevron. When expanded, a horizontal carousel renders glassmorphic document cards showing format badges, relative access time (e.g., *"Just now"*, *"4m ago"*), sync status icon, and quick-open action.
  2. **Dedicated Storage Dashboard Tab**:
     - Accessible from the primary workspace navigation bar alongside **Workspace** and **Batch Studio**.
     - Features an animated, custom Canvas **Interactive Donut Chart** with proportional arcs for each format (`PDF`, `DOCX`, `PPTX`, `XLSX`, `PIC`).
     - Tapping a donut slice or legend card selects that format, displaying exact byte size, document count, percentage of total footprint, and a **"Filter Workspace by Format"** action button.
     - Includes an Offline Cache & Encrypted Vault meter with a one-tap *"Compact & Prune Old Snapshots"* utility.
  3. **Google-Docs-Style Live Editor**:
     - **Top Collaboration Bar**: Displays a live Sync Status Pill (`Saved` • `Saving...` • `Offline — changes will sync`), an avatar stack of active collaborators with colored presence rings, and quick triggers for **Comments**, **Version History**, **Suggesting Mode**, and **Share / Access Roles**.
     - **Live Canvas & Colored Cursors**: Every keystroke or block change updates the local CRDT/document state and triggers sub-second encrypted sync without moving the local cursor. Collaborator positions appear as colored name flags inside active blocks.
     - **Comments & Suggesting Drawer**: Allows anchoring threaded comments to specific blocks/text with `@mentions` and resolution tracking, plus toggling between **Editing**, **Suggesting** (tracked insertions/deletions with owner Accept/Reject buttons), and **Viewing** modes.
     - **Version History & Share Modals**: View automatic and named snapshots with one-tap non-destructive restore (creating a new version without erasing history), and manage collaborator roles (`Owner`, `Editor`, `Commenter`, `Viewer`) with client-side AES-256-GCM key wrapping and link expiry controls.

---

## 3. Key Product Decisions & Trade-Offs
- **Conflict-Free Offline Merging vs. Traditional File Locking**:
  - Instead of locking documents or generating duplicate *"conflict copy"* files when two devices edit offline, edits are modeled as granular block/delta operations with state vectors. When an offline device reconnects, pending local deltas merge cleanly with remote updates in chronological/vector order.
- **Client-Side E2E Encryption with On-Device Search**:
  - All live updates and snapshots are encrypted with the document's AES-256-GCM key before leaving the device. Because cloud storage only holds ciphertext, full-text search runs locally at sub-millisecond speed against the decrypted Room FTS4 index (`cached_documents_fts`).
- **Role Enforcement**:
  - Collaborator permissions (`Owner`, `Editor`, `Commenter`, `Viewer`) are enforced both in the editor UI (automatically switching `Viewer` users to read-only mode and `Commenter` users to comment/suggestion-only mode) and in backend security validation.

---

## 4. Technical Architecture & Data Strategy

```
+-------------------------------------------------------------------------+
|                   DocFlow Crystal Glass UI Layer                        |
|                                                                         |
|  +-----------------------+  +-------------------+  +-----------------+  |
|  | Workspace & Expandable|  | Dedicated Storage |  | Live Document   |  |
|  | Recent Files Drawer   |  | Donut Chart Tab   |  | Collaborative   |  |
|  +-----------+-----------+  +---------+---------+  | Editor          |  |
|              |                        |            +--------+--------+  |
+--------------|------------------------|---------------------|-----------+
               |                        |                     |
               v                        v                     v
+-------------------------------------------------------------------------+
|                 DocFlowViewModel & Live Collaboration Engine            |
|  - Sub-second debounced keystroke delta emitter & state vector merger   |
|  - Active collaborator presence, colored cursors & role enforcement     |
|  - Storage format aggregator (PDF / DOCX / PPTX / XLSX / PIC bytes)     |
+--------------------------+----------------------------------+-----------+
                           |                                  |
         Instant Local I/O |                                  | AES-256-GCM
        & FTS4 Query (<5ms)|                                  | Ciphertext Sync
                           v                                  v
+-------------------------------------+    +------------------------------+
|     Room Offline-First Database     |    |  Cloud Realtime & Firestore  |
|  - cached_documents (+lastAccessed) |    |  - Encrypted delta updates   |
|  - cached_documents_fts (FTS4)      |    |  - Compacted snapshots       |
|  - document_versions (snapshots)    |    |  - Collaborator presence &   |
|  - document_comments & suggestions  |    |    wrapped document keys     |
+-------------------------------------+    +------------------------------+
```

- **Room Database Enhancements**:
  - Extend `CachedDocumentEntity` with `lastAccessedAtMillis` and add DAO queries (`observeRecentCachedDocuments`, `updateDocumentLastAccessed`) to reactive-stream the most recently accessed documents into the expandable drawer.
  - Add local entities and DAO queries for **Version History Snapshots** (`DocumentVersionEntity`) and **Inline Comments / Tracked Suggestions** (`DocumentCommentEntity`) so comments, suggestions, and version restores work seamlessly offline and sync automatically when online.
- **Storage Aggregation Pipeline**:
  - Compute real-time byte totals, file counts, and percentages grouped by normalized format category (`PDF`, `DOCX`, `PPTX`, `XLSX`, `PIC`) directly from the reactive document stream, feeding the interactive Canvas Donut Chart and one-tap format filter.
- **Live Sync & Snapshot Compaction**:
  - Every keystroke/block edit immediately updates the in-memory state and Room cache, displays the `Saving...` -> `Saved` indicator in under a second, and pushes encrypted updates to cloud storage. Automatic snapshot compaction consolidates updates into version checkpoints and allows instant restoration of any historical state.
