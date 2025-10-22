# Code Structure

## Project Layout
```
niconico-advertiser-list2/
├── site/                                    # Main application subproject
│   ├── src/jsMain/                         # Kotlin/JS source code
│   │   ├── kotlin/net/numa08/niconico_advertiser_list2/
│   │   │   ├── AppEntry.kt                 # Application entry point (@App)
│   │   │   ├── SiteTheme.kt                # Theme configuration
│   │   │   ├── AppStyles.kt                # Global styles
│   │   │   ├── pages/                      # Page components
│   │   │   │   └── Index.kt                # Home page (@Page)
│   │   │   └── components/                 # Reusable components
│   │   │       ├── layouts/                # Layout components
│   │   │       │   ├── PageLayout.kt       # Main page layout (@Layout)
│   │   │       │   └── MarkdownLayout.kt   # Markdown page layout
│   │   │       ├── sections/               # Section components
│   │   │       │   ├── NavHeader.kt        # Navigation header
│   │   │       │   └── Footer.kt           # Footer section
│   │   │       └── widgets/                # Widget components
│   │   │           └── IconButton.kt       # Icon button widget
│   │   └── resources/
│   │       ├── markdown/                   # Markdown content
│   │       │   └── About.md                # About page
│   │       └── public/                     # Static assets
│   │           ├── favicon.ico
│   │           └── kobweb-logo.png
│   └── build.gradle.kts                    # Site subproject build config
├── gradle/                                 # Gradle configuration
│   └── libs.versions.toml                  # Version catalog
├── settings.gradle.kts                     # Multi-project settings
└── gradle.properties                       # Gradle properties

```

## Key Components

### Entry Point
- **AppEntry.kt**: Main application entry point annotated with `@App`
  - Initializes SilkApp
  - Handles ColorMode (dark/light theme)
  - Provides Surface wrapper for all pages

### Pages
- **Index.kt (HomePage)**: Main landing page with hero section and grid layout
  - Annotated with `@Page` and `@Layout`
  - Uses PageLayout for consistent structure

### Layouts
- **PageLayout.kt**: Shared layout for most pages
  - Contains NavHeader, page content area, and Footer
  - Uses CSS Grid for layout management
- **MarkdownLayout.kt**: Layout for markdown-based pages

### Theme
- **SiteTheme.kt**: Custom color palette definitions
  - SitePalette class: nearBackground, cobweb, brand colors
  - Supports light/dark color modes
- **AppStyles.kt**: Global style definitions
  - Headline and subheadline text styles
  - Button variants (CircleButton, UncoloredButton)

### Components
- **NavHeader.kt**: Navigation header with menu items, color mode toggle, and responsive hamburger menu
- **Footer.kt**: Footer section
- **IconButton.kt**: Reusable icon button widget

## Package Structure
All source code is under the package: `net.numa08.niconico_advertiser_list2`

## Architectural Patterns
- **Component-based architecture**: Composable functions for UI components
- **Layout composition**: Layouts wrap page content for consistent structure
- **Annotation-driven routing**: `@Page` annotation for automatic route discovery
- **Theme system**: Centralized color palette with dark/light mode support
