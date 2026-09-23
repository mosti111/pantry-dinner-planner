import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Pantry — Dinner, sorted",
  description: "Plan several dinners and one honest whole-package grocery basket.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
