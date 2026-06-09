/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f4f8f7',
          100: '#dbe9e4',
          200: '#b9d4ca',
          300: '#90b8a8',
          400: '#669b88',
          500: '#477e6d',
          600: '#386557',
          700: '#2f5147',
          800: '#29423a',
          900: '#253732',
        },
        accent: {
          50: '#fdf3ec',
          100: '#fbe4d4',
          200: '#f6c6a8',
          300: '#efa06f',
          400: '#e97e45',
          500: '#e26025',
          600: '#d34a1c',
          700: '#af3819',
          800: '#8d2e1b',
          900: '#722818',
        },
      },
      boxShadow: {
        panel: '0 18px 50px rgba(20, 30, 26, 0.08)',
      },
      fontFamily: {
        sans: ['ui-sans-serif', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
