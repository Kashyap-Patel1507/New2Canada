// Shared Tailwind config for the New2Canada design system (generated via Stitch).
// Include this AFTER the Tailwind CDN <script> tag on every page that uses it.
tailwind.config = {
    darkMode: "class",
    theme: {
        extend: {
            colors: {
                "on-primary-container": "#fdfcff",
                "surface-variant": "#dfe3e8",
                "surface-container-low": "#f1f4fa",
                "secondary-fixed": "#89f5e7",
                "surface-container-highest": "#dfe3e8",
                "surface-tint": "#006398",
                "surface-container": "#ebeef4",
                "on-secondary": "#ffffff",
                "surface-dim": "#d7dae0",
                "on-error-container": "#93000a",
                "on-tertiary": "#ffffff",
                "on-tertiary-fixed": "#002109",
                "surface-bright": "#f7f9ff",
                "surface": "#f7f9ff",
                "tertiary-fixed": "#6bff8f",
                "on-background": "#181c20",
                "error-container": "#ffdad6",
                "error": "#ba1a1a",
                "on-secondary-container": "#006f66",
                "on-primary": "#ffffff",
                "on-error": "#ffffff",
                "on-secondary-fixed": "#00201d",
                "tertiary-container": "#00873b",
                "on-surface": "#181c20",
                "on-tertiary-container": "#f7fff3",
                "tertiary-fixed-dim": "#4ae176",
                "primary-fixed": "#cce5ff",
                "secondary-container": "#86f2e4",
                "inverse-surface": "#2d3135",
                "on-secondary-fixed-variant": "#005049",
                "background": "#f7f9ff",
                "outline": "#707881",
                "on-surface-variant": "#3f4850",
                "primary-fixed-dim": "#93ccff",
                "surface-container-high": "#e5e8ee",
                "secondary-fixed-dim": "#6bd8cb",
                "surface-container-lowest": "#ffffff",
                "tertiary": "#006b2d",
                "secondary": "#006a61",
                "inverse-on-surface": "#eef1f7",
                "inverse-primary": "#93ccff",
                "primary-container": "#007bb9",
                "on-primary-fixed": "#001d31",
                "outline-variant": "#bfc7d2",
                "primary": "#006194",
                "on-primary-fixed-variant": "#004b73",
                "on-tertiary-fixed-variant": "#005321"
            },
            borderRadius: {
                DEFAULT: "0.25rem",
                lg: "0.5rem",
                xl: "0.75rem",
                full: "9999px"
            },
            spacing: {
                "margin-mobile": "20px",
                "base": "4px",
                "gutter": "24px",
                "container-max": "1280px",
                "margin-desktop": "48px"
            },
            fontFamily: {
                "body-md": ["Inter"],
                "headline-sm": ["Plus Jakarta Sans"],
                "body-lg": ["Inter"],
                "label-sm": ["Inter"],
                "body-sm": ["Inter"],
                "label-md": ["Inter"],
                "headline-lg": ["Plus Jakarta Sans"],
                "headline-lg-mobile": ["Plus Jakarta Sans"],
                "headline-md": ["Plus Jakarta Sans"]
            },
            fontSize: {
                "body-md": ["16px", { lineHeight: "1.5", fontWeight: "400" }],
                "headline-sm": ["20px", { lineHeight: "1.4", fontWeight: "600" }],
                "body-lg": ["18px", { lineHeight: "1.6", fontWeight: "400" }],
                "label-sm": ["12px", { lineHeight: "1", fontWeight: "500" }],
                "body-sm": ["14px", { lineHeight: "1.5", fontWeight: "400" }],
                "label-md": ["14px", { lineHeight: "1", letterSpacing: "0.01em", fontWeight: "600" }],
                "headline-lg": ["40px", { lineHeight: "1.2", letterSpacing: "-0.02em", fontWeight: "700" }],
                "headline-lg-mobile": ["32px", { lineHeight: "1.2", letterSpacing: "-0.01em", fontWeight: "700" }],
                "headline-md": ["24px", { lineHeight: "1.3", fontWeight: "600" }]
            }
        }
    }
};
