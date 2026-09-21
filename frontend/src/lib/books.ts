import type { Book, Experiment } from "./experiment";

// 사용자 제공 판매 페이지의 제목, 저자와 실제 표지 이미지.
export const collectedBooks: Book[] = [
  // 사용자 결정: 판매 분류와 별개로 실험에서는 고전소설로 집계한다.
  { id: "odyssey", title: "오디세이", author: "김상근", genre: "고전소설",
    coverUrl: "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791169852210.jpg", productUrl: "https://product.kyobobook.co.kr/detail/S000220663823" },
  { id: "cocktail-love-zombies", title: "칵테일, 러브, 좀비", author: "조예은", genre: "현대소설",
    coverUrl: "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791190174756.jpg", productUrl: "https://product.kyobobook.co.kr/detail/S000001936854" },
  { id: "justice", title: "정의란 무엇인가", author: "마이클 샌델", genre: "철학",
    coverUrl: "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9788937834790.jpg", productUrl: "https://product.kyobobook.co.kr/detail/S000000625441" },
  { id: "my-funeral", title: "내가 죽으면 장례식에 누가 와줄까", author: "김상현", genre: "에세이",
    coverUrl: "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791188469468.jpg", productUrl: "https://product.kyobobook.co.kr/detail/S000001914822" },
  { id: "alya-1", title: "가끔씩 툭하고 러시아어로 부끄러워하는 옆자리의 아랴 양 1", author: "SUN SUN SUN", genre: "서브컬쳐",
    coverUrl: "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791127864408.jpg", productUrl: "https://product.kyobobook.co.kr/detail/S000001673408" },
  { id: "love-on-the-rock", title: "러브 온 더 락", author: "고선경", genre: "시집",
    coverUrl: "https://image.aladin.co.kr/product/39066/11/cover500/8936425358_1.jpg", productUrl: "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=390661137" },
];

export function withCollectedBooks(experiment: Experiment): Experiment {
  const collectedIds = new Set(collectedBooks.map((book) => book.id));
  return { ...experiment, books: [...collectedBooks.map((book) => ({ ...book })), ...experiment.books.filter((book) => !collectedIds.has(book.id))] };
}
