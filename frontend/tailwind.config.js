/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        pine: {
          DEFAULT: "#16332f",
          deep: "#0e2421",
          soft: "#e5f0ec",
        },
        paper: "#f3eee6",
        card: "#fffdf9",
        line: "#e3d8c8",
        copper: {
          DEFAULT: "#b8613a",
          dark: "#8c4526",
        },
        ink: "#1c1915",
        muted: "#6f675e",
      },
      fontFamily: {
        sans: ['"Source Sans 3"', "ui-sans-serif", "system-ui", "sans-serif"],
        serif: ['"Source Serif 4"', "Georgia", "serif"],
      },
      boxShadow: {
        card: "0 16px 40px rgba(28, 25, 21, 0.08)",
      },
    },
  },
  plugins: [],
};
