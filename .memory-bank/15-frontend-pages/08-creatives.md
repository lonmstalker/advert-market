# Creatives

> Creative library — user-owned ad templates with Telegram-like preview. Accessed from Profile tab.

## Navigation

```
/profile
  └── /profile/creatives
      ├── /profile/creatives/new
      └── /profile/creatives/:creativeId/edit
```

---

## New API endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/creatives` | List user's templates (cursor pagination) | Authenticated |
| `POST` | `/api/v1/creatives` | Create template | Authenticated |
| `GET` | `/api/v1/creatives/{id}` | Get template | Authenticated |
| `PUT` | `/api/v1/creatives/{id}` | Update template | Authenticated |
| `DELETE` | `/api/v1/creatives/{id}` | Soft delete | Authenticated |
| `GET` | `/api/v1/creatives/{id}/versions` | Version history | Authenticated |
| `POST` | `/api/v1/creatives/import-from-deal` | Import from deal | Authenticated |
| `POST` | `/api/v1/deals/{dealId}/creative/import-from-library` | Import to deal | Authenticated |

### New query keys (add to `query-keys.ts`)

```typescript
export const creativeLibraryKeys = {
  all: ['creative-library'] as const,
  lists: () => [...creativeLibraryKeys.all, 'list'] as const,
  list: (params?: CursorParams) => [...creativeLibraryKeys.lists(), params] as const,
  details: () => [...creativeLibraryKeys.all, 'detail'] as const,
  detail: (id: string) => [...creativeLibraryKeys.details(), id] as const,
  history: (id: string) => [...creativeLibraryKeys.detail(id), 'history'] as const,
};
```

---

## 8.1 Creative library list

| | |
|---|---|
| **Route** | `/profile/creatives` |
| **Target** | User's saved creative templates |
| **Who sees** | All authorized |

### API

```
GET /api/v1/creatives?cursor=&limit=20
```

**Query keys:** `creativeLibraryKeys.list(params)`

### UI

- **Header**: `t('creatives.title')` (`title1 bold`)
- **List of creatives** — `Group` + `GroupItem`:
  - `before`: TelegramPostPreview thumbnail (40x40, showing first media or text icon)
  - Title: template title
  - `description`: first line of text, truncated
  - `after`: `t('creatives.mediaCount', { count })` if has media
  - `chevron`
- **Infinite scroll** — skeleton loading
- **Sort** by `updatedAt` (desc)

### Actions

| Action | Result |
|--------|--------|
| Tap creative | → `/profile/creatives/:creativeId/edit` |
| FAB "+" button | → `/profile/creatives/new` |
| Swipe left (or long press) | Delete confirmation → `DELETE /api/v1/creatives/:id` |
| Pull-to-refresh | Invalidate `creativeLibraryKeys.lists()` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `🎨` | `creatives.empty.title` | `creatives.empty.description` | `creatives.empty.cta` → `/profile/creatives/new` |

### Error states

| Error | UI |
|-------|---|
| Loading Error | `ErrorScreen` + retry |
| Offline | Banner `t('errors.offline')` |

---

## 8.2 Creative editor (create / edit)

| | |
|---|---|
| **Route** | `/profile/creatives/new` and `/profile/creatives/:creativeId/edit` |
| **Target** | Create or edit a creative template with real-time Telegram preview |
| **Who sees** | All authorized (own templates only) |

### API

```
GET /api/v1/creatives/:creativeId          # Edit mode: load existing
POST /api/v1/creatives                     # Create mode
PUT /api/v1/creatives/:creativeId          # Edit mode: save
```

**Query keys:** `creativeLibraryKeys.detail(creativeId)`

### UI — Two-panel layout

Split via `SegmentControl`: `t('creatives.editor.edit')` / `t('creatives.editor.preview')`

#### Edit panel

- **Input `t('creatives.editor.title')`** — text, max 100 chars, required
- **SegmentControl `t('creatives.editor.parseMode')`** — `MarkdownV2` / `HTML`
- **TextArea `t('creatives.editor.text')`** — max 4096 chars (Telegram limit)
  - Character counter: `{length}/4096`
  - Formatting toolbar (optional, v2): bold, italic, underline, strikethrough, code, link
- **Group `t('creatives.editor.media')`** — dynamic list:
  - Each item: Input URL + delete button (×)
  - Button `t('creatives.editor.addMedia')` (`link`) — max 10
  - V2: file upload with drag & drop
- **Group `t('creatives.editor.buttons')`** — inline keyboard builder:
  - Each row: Input `t('creatives.editor.buttonText')` (50 chars) + Input `t('creatives.editor.buttonUrl')` + delete (×)
  - Button `t('creatives.editor.addButton')` (`link`) — max 5
- **Toggle `t('creatives.editor.disablePreview')`** — web page preview on/off
- Button `t('common.save')` (`primary`, full-width, bottom)

#### Preview panel

- **TelegramPostPreview** component with live data from form state
- Channel name: `t('creatives.preview.channelName')` (placeholder "My Channel")
- Editable channel name via small input above preview (optional, for accurate preview)

### Data model: text + entities (Telegram Bot API approach)

Text and formatting are separated. Backend converts text + entities to MarkdownV2 with proper escaping for Telegram API.

```typescript
// Formatting entity types (enum)
const TextEntityType = {
  BOLD: 'BOLD',
  ITALIC: 'ITALIC',
  UNDERLINE: 'UNDERLINE',
  STRIKETHROUGH: 'STRIKETHROUGH',
  SPOILER: 'SPOILER',
  CODE: 'CODE',
  PRE: 'PRE',
  TEXT_LINK: 'TEXT_LINK',
} as const;

type TextEntity = {
  type: TextEntityType;
  offset: number;        // UTF-16 offset in text
  length: number;        // UTF-16 length
  url?: string;          // TEXT_LINK only
  language?: string;     // PRE only
};
```

### Form state management

Local state via `useState` (not Zustand — max 1 store rule):
```typescript
{
  title: string;
  text: string;                  // plain text (no formatting markers)
  entities: TextEntity[];        // formatting annotations
  media: MediaItem[];            // { type, fileId, caption?, url? }
  buttons: InlineButton[];       // { text, url }
  disableWebPagePreview: boolean;
}
```

Reset on mount (create) or populate from API (edit).

### Formatting toolbar

Select text in textarea → click toolbar button → adds/toggles TextEntity:
- **B** (bold), **I** (italic), **U** (underline), **S** (strikethrough)
- `</>` (code), link icon (opens URL input)
- Uses `textarea.selectionStart` / `selectionEnd`
- Active entities highlight corresponding toolbar buttons

### Validation (Zod)

```typescript
const creativeFormSchema = z.object({
  title: z.string().min(1).max(100),
  text: z.string().min(1).max(4096),
  entities: z.array(z.object({
    type: z.nativeEnum(TextEntityType),
    offset: z.number().min(0),
    length: z.number().min(1),
    url: z.string().url().optional(),
    language: z.string().optional(),
  })).default([]),
  media: z.array(z.object({
    type: z.enum(['PHOTO', 'VIDEO', 'GIF', 'DOCUMENT']),
    fileId: z.string(),
    caption: z.string().optional(),
  })).max(10),
  buttons: z.array(z.object({
    text: z.string().min(1).max(50),
    url: z.string().url(),
  })).max(5),
});
```

### Request body

```typescript
{
  title: string;
  text: string;                  // plain text
  entities: TextEntity[];        // formatting annotations
  media: MediaItem[];            // file_id based
  buttons: InlineButton[];
  disableWebPagePreview: boolean;
}
```

### Actions

| Action | Result |
|--------|--------|
| Switch Edit/Preview | SegmentControl toggles panels |
| "Save" (create) | `POST /api/v1/creatives` → navigate `/profile/creatives` + toast `t('creatives.toast.created')` |
| "Save" (edit) | `PUT /api/v1/creatives/:id` → navigate `/profile/creatives` + toast `t('creatives.toast.saved')` |
| BackButton | Unsaved changes → `DialogModal` confirmation |

### Error states

| Error | UI |
|-------|---|
| Save error | Toast `t('common.toast.saveFailed')` |
| 404 template not found | `ErrorScreen` `t('errors.notFound.title')` + navigate `/profile/creatives` |
| Validation | Inline errors on inputs |

---

## 8.3 Creative version history

| | |
|---|---|
| **Route** | Sheet on `/profile/creatives/:creativeId/edit` |
| **Target** | View template change history |
| **Who sees** | Template owner |

### API

```
GET /api/v1/creatives/:creativeId/versions
```

**Query keys:** `creativeLibraryKeys.history(creativeId)`

### UI

- **Sheet** opened via `GroupItem` "History" button on edit page
- **Timeline list** — chronological (newest first):
  - Each item: version number + date + text preview (first 50 chars)
  - `chevron` → replaces editor content with this version (with confirmation)
- Footer: `t('creatives.history.count', { count })`

### Actions

| Action | Result |
|--------|--------|
| Tap version | `DialogModal` "Restore this version?" → populate editor form |
| Close sheet | Dismiss |

---

## TelegramPostPreview — shared component

> Location: `src/shared/ui/components/telegram-post-preview/`

### Props

```typescript
type TelegramPostPreviewProps = {
  text: string;                 // plain text (no formatting markers)
  entities: TextEntity[];       // formatting annotations
  media: MediaItem[];           // { type, fileId, caption?, url? }
  buttons: InlineButton[];      // { text, url }
  channelTitle?: string;
  channelAvatar?: string;
  disableWebPagePreview?: boolean;
};
```

### Visual structure

```
┌──────────────────────────────────────────┐
│ [●] Channel Name                  12:34  │  ← TelegramPostHeader
├──────────────────────────────────────────┤
│ ┌──────────────────────────────────────┐ │
│ │          Media Gallery               │ │  ← TelegramPostMedia
│ └──────────────────────────────────────┘ │
│                                          │
│  Formatted **bold** and _italic_ text    │  ← TelegramPostBody
│  with [links](https://example.com) and `code`... │
│                                          │
│ ┌──────────┐ ┌──────────┐               │  ← TelegramPostButtons
│ │ Button 1 │ │ Button 2 │               │
│ └──────────┘ └──────────┘               │
│ ┌──────────┐                            │
│ │ Button 3 │                            │
│ └──────────┘                            │
└──────────────────────────────────────────┘
```

### Styling rules

- Container: `borderRadius: 12px`, `background: var(--color-background-base)`
- Shadow: `0 1px 2px rgba(0,0,0,0.08)` (light) / `none` (dark)
- Padding: `12px`
- Channel name: `subheadline2 medium`, color `accent`
- Time: `caption2 secondary`, right-aligned
- Text: `body regular`, preserves whitespace and newlines
- Buttons: `borderRadius: 8px`, `background: var(--color-accent-primary)`, `color: var(--color-static-white)`, `padding: 8px 16px`, centered text
- Media: `borderRadius: 8px`, `object-fit: cover`, max-height `300px`

### MarkdownV2 parser

Pure function `parseMarkdownV2(text: string): ReactNode[]`

Supported formatting:
- `*bold*` → `<strong>`
- `_italic_` → `<em>`
- `__underline__` → `<u>`
- `~strikethrough~` → `<del>`
- `||spoiler||` → click-to-reveal `<span>`
- `` `code` `` → `<code>`
- ```` ```pre``` ```` → `<pre><code>`
- `[text](https://example.com)` → `<a>`
- `\` escape character

Tests in `telegram-markdown-parser.test.ts` covering all formatting combinations.

### HTML parser

Uses `DOMParser` to render HTML formatting (Telegram HTML subset):
- `<b>`, `<strong>` → bold
- `<i>`, `<em>` → italic
- `<u>`, `<ins>` → underline
- `<s>`, `<strike>`, `<del>` → strikethrough
- `<code>`, `<pre>` → code blocks
- `<a href="...">` → links
- `<tg-spoiler>` → click-to-reveal

---

## Integration with deals

### Deal creative page (3.5) enhancement

On `/deals/:dealId/creative`:
- Add "Import from library" button (`secondary`)
- Opens Sheet with user's creative templates list
- Selecting a template populates the form

### Deal creative review (3.6) enhancement

On `/deals/:dealId/creative/review`:
- Replace plain text preview with `TelegramPostPreview` component
- Add "Save to library" button (`secondary`, small)

### Deal creative history

On `/deals/:dealId` in Creative group:
- Add `GroupItem` "Creative history" with version count badge → opens Sheet with timeline

### Both parties editing

- In `FUNDED` / `CREATIVE_SUBMITTED` states, both parties see "Edit" button on creative section
- Each edit records a `deal_event` with `event_type = 'CREATIVE_PROPOSAL'`
- History shows all proposals with author badge (advertiser/owner)

---

## File structure

```
src/shared/ui/components/
  telegram-post-preview/
    TelegramPostPreview.tsx
    TelegramPostHeader.tsx
    TelegramPostBody.tsx
    TelegramPostMedia.tsx
    TelegramPostButtons.tsx
    TelegramSpoiler.tsx
    telegram-markdown-parser.ts
    telegram-markdown-parser.test.ts
    telegram-html-parser.ts
    telegram-html-parser.test.ts
    styles.ts                        # CSS-in-JS style objects
    index.ts

src/features/creatives/
  api/creatives-api.ts
  components/
    CreativeListItem.tsx
    CreativeForm.tsx
    CreativeHistorySheet.tsx
    ImportCreativeSheet.tsx
    ButtonBuilder.tsx
    MediaUrlList.tsx
  hooks/
    useCreativeDetail.ts
    useCreatives.ts
    useCreativeMutations.ts
  stores/
    creative-editor-store.ts
  types/creative.ts
  index.ts

src/pages/creatives/
  CreativesPage.tsx                   # Route: /profile/creatives
  CreativeEditorPage.tsx              # Route: /profile/creatives/new + :id/edit
```

---

## i18n keys

Example values (en):

```
creatives.title: "My Creatives"
creatives.empty.title: "No creatives"
creatives.empty.description: "Create an ad post template"
creatives.empty.cta: "Create creative"
creatives.mediaCount: "{{count}} media"
creatives.editor.edit: "Editor"
creatives.editor.preview: "Preview"
creatives.editor.title: "Title"
creatives.editor.parseMode: "Format"
creatives.editor.text: "Post text"
creatives.editor.media: "Media"
creatives.editor.addMedia: "Add media"
creatives.editor.buttons: "Buttons"
creatives.editor.buttonText: "Button text"
creatives.editor.buttonUrl: "Link"
creatives.editor.addButton: "Add button"
creatives.editor.disablePreview: "Disable link preview"
creatives.preview.channelName: "My Channel"
creatives.toast.created: "Creative created"
creatives.toast.saved: "Creative saved"
creatives.toast.deleted: "Creative deleted"
creatives.history.title: "Version history"
creatives.history.count: "{{count}} versions"
creatives.history.restore: "Restore this version?"
creatives.importFromLibrary: "From library"
creatives.saveToLibrary: "Save to library"
```

---

## Related Documents

- [Profile](./05-profile.md) — entry point via "My Creatives" GroupItem
- [Deals — Creative](./03-deals.md#35-submitting-creative) — deal creative submission
- [Deals — Review](./03-deals.md#36-creative-review) — deal creative review
- [Creative JSONB Schemas](../14-implementation-specs/20-creative-jsonb-schemas.md)
- [Creative Workflow](../03-feature-specs/03-creative-workflow.md)
