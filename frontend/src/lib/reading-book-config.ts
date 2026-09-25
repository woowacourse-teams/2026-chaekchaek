export type ReadingBookId = "stranger" | "love-fragments";

export const LOVE_AUTHOR_ID = "love-fragments:initial-author";
export const LOVE_ID_PREFIX = "love-fragments:";

export const readingBooks = {
  stranger: {
    id: "stranger",
    title: "이방인",
    coverUrl: "https://image.yes24.com/goods/192097306/XL",
    excerptTitle: "발췌문 99-104쪽",
    excerptDescription: "책에 적힌 필기와 밑줄도 함께 볼 수 있어요.",
    context: "뫼르소는 범죄 사건에 연루되어, 피의자 신분으로 법정 재판에 참석하게 된다. 재판이 시작되었지만, 뫼르소의 ‘범죄 행위’ 그 자체보다는 지난 어머니 장례식에서의 태도와 평소 행실을 끌어와 그를 비난하기 시작한다.",
    printedPages: [99, 100, 101, 102, 103, 104],
    scanWidth: 960,
    scanHeight: 1440,
    alignFacingPages: true,
    initialAuthorId: "stranger-initial-author",
  },
  "love-fragments": {
    id: "love-fragments",
    title: "사랑의 편린들",
    coverUrl: "https://contents.kyobobook.co.kr/sih/fit-in/400x0/pdt/9791199633414.jpg?t=2983840",
    excerptTitle: "발췌문 34-36쪽, 86-88쪽",
    excerptDescription: "두 부분의 발췌문을 차례로 읽어보세요.",
    context: null,
    printedPages: [34, 35, 36, 86, 87, 88],
    scanWidth: 993,
    scanHeight: 1404,
    alignFacingPages: false,
    initialAuthorId: LOVE_AUTHOR_ID,
  },
} as const;

export function belongsToReadingBook(book: ReadingBookId, id: string) {
  return book === "love-fragments" ? id.startsWith(LOVE_ID_PREFIX) : !id.startsWith(LOVE_ID_PREFIX);
}

export function newReadingId(book: ReadingBookId) {
  return (book === "love-fragments" ? LOVE_ID_PREFIX : "") + crypto.randomUUID();
}
