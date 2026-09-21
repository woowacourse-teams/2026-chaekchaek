import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "첵췍 | 책과 감상",
  description: "책을 고르고, 마음에 남은 문장과 생각을 나눠보세요.",
  robots: { index: false, follow: false },
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="ko"><body>{children}</body></html>;
}
