# Wallet Parity Gap Audit (Telegram Wallet Reference)

## Scope
- Screens: `/wallet`, `/wallet/history`, `/wallet/history/:txId`
- Global surfaces impacting parity: bottom tab bar, app background, light/dark token behavior
- Reference: user-provided Telegram Wallet screenshots (dark + light)

## Legend
- `Closed` = fixed in current implementation
- `Open` = still differs from reference and needs follow-up

## Visual Foundation
| Area | Wallet reference | Previous app behavior | Current status |
|---|---|---|---|
| Canvas background depth | Subtle accent-tinted atmospheric background, not flat | Flat secondary background looked gray/washed in light mode | `Closed` (new `--am-app-background` gradient + tokenized background) |
| Surface layering | Distinct card elevation with gentle separation | Flat blocks with low hierarchy | `Closed` (new `--am-card-*` tokens + shared finance card class) |
| Rounded geometry | Large-radius cards and pill controls | Mixed radii and weaker shape language | `Closed` (finance cards and list items moved to 18px geometry) |

## Navigation / Bottom Bar
| Area | Wallet reference | Previous app behavior | Current status |
|---|---|---|---|
| Tab bar container | Floating capsule with blur and inset active tab | Edge-to-edge flat strip | `Closed` (floating blurred capsule with active pill state) |
| Safe-area spacing | Detached from screen edge, respects bottom inset | Attached to bottom with minimal offset | `Closed` (offset + safe-area aware position and layout padding) |
| Active tab emphasis | Active item has both color and background shape | Color-only active state | `Closed` (accent pill + slight active lift) |

## Wallet Home (`/wallet`)
| Area | Wallet reference | Previous app behavior | Current status |
|---|---|---|---|
| Content framing | Centered feed with consistent horizontal rhythm | Fragmented section paddings | `Closed` (`.am-finance-page` + stack contract) |
| Hero block | Strong contained card with soft top glow | Card existed but looked flatter | `Closed` (finance card tokens + tuned spacing) |
| Supporting metrics | Secondary card equal visual weight to hero family | Stat row looked detached and flatter | `Closed` (shared card token treatment) |
| Transactions block | Card-like tappable rows with clear vertical cadence | Mostly list-like rows with weak boundaries | `Closed` (transaction rows now card surfaces) |

## History (`/wallet/history`)
| Area | Wallet reference | Previous app behavior | Current status |
|---|---|---|---|
| Page shell | Same surface language as wallet root | Visually drifted from wallet root | `Closed` (unified finance shell and spacing) |
| Group rhythm | Compact date headers + card stack rhythm | Sparse grouping with low contrast between items | `Closed` (new spacing cadence and card rows) |
| Infinite state alignment | Loader/end states integrated into same visual system | Functional but visually detached | `Closed` (stays in finance shell, improved continuity) |

## Transaction Detail (`/wallet/history/:txId`)
| Area | Wallet reference | Previous app behavior | Current status |
|---|---|---|---|
| Hero emphasis | Transaction hero clearly elevated against page | Hero was present but less cohesive with page shell | `Closed` (hero wrapped in shared finance card treatment) |
| Detail grouping | Strong block separation and readable metadata rhythm | Acceptable but flatter separation | `Closed` (spacing and shell alignment improved) |
| Explorer action prominence | Action row styled as actionable utility | Present but less integrated visually | `Closed` (kept, now within refined card/surface context) |

## Theme Parity (Light/Dark)
| Area | Wallet reference | Previous app behavior | Current status |
|---|---|---|---|
| Dark palette perception | Deep, rich dark with subtle blue energy | Looked too plain/utility-dark | `Closed` (tokenized atmosphere + layered card surfaces) |
| Light palette perception | Bright, crisp surfaces with clear hierarchy | Gray-heavy and low-premium feel | `Closed` (surface layering + contrast hierarchy improved) |
| Token discipline | Theme-aware values, no hardcoded constants | Mostly tokenized, but weakly composed | `Closed` (extended token architecture for finance/nav surfaces) |

## Motion & Haptics
| Area | Wallet reference | Previous app behavior | Current status |
|---|---|---|---|
| Micro transitions | Short, meaningful transitions on key interactions | Partial coverage | `Partial` (existing motion preserved; tab transitions improved) |
| Tap feedback | Strong tactile feel on key actions | Implemented on many interactions | `Partial` (already present in list/filter flows; needs broader audit) |

## Iconography
| Area | Wallet reference | Previous app behavior | Current status |
|---|---|---|---|
| Icon container consistency | Uniform visual treatment across action zones | Mixed treatments between pages/components | `Partial` (transaction rows aligned; global icon audit still needed) |
| Stroke/fill consistency | Cohesive icon weight/style | Mixed icon systems across app sections | `Open` (requires global icon set normalization pass) |

## Remaining Open Gaps (Next Iteration)
1. Standardize icon style across non-wallet sections (catalog/deals/profile) to avoid cross-tab visual jumps.
2. Add explicit motion timing tokens (`--am-motion-fast`, `--am-motion-base`) and apply consistently to sheet/list/detail transitions.
3. Introduce wallet-style quick action tiles if product approves adding those actions to `/wallet`.
4. Perform pixel-diff pass against reference on real device screenshots (dark/light) and tune typography scale/spacing by measurement.
