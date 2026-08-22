## 도메인 컴포넌트 규칙

도메인 컴포넌트는 `components` 폴더 내부에 컴포넌트 이름과 동일한 이름의 폴더를 만들어 관리한다.

도메인 컴포넌트의 기본 파일 구조는 다음 규칙을 따른다.

components/
└── <DomainComponentName>/
    ├── index.ts
    ├── <DomainComponentName>.tsx
    └── <DomainComponentName>.types.ts

`<DomainComponentName>.tsx`에는 도메인 컴포넌트의 구현 코드를 작성한다.

`<DomainComponentName>Props` 타입은 `<DomainComponentName>.tsx` 내부에 직접 선언하지 않고 `<DomainComponentName>.types.ts`에 선언한다.

`index.ts`에서는 해당 도메인 컴포넌트의 외부 공개 인터페이스를 export한다.

예시:

components/
└── BookOverview/
    ├── index.ts
    ├── BookOverview.tsx
    └── BookOverview.types.ts

// BookOverview.types.ts
export type BookOverviewProps = {
  ...
};

// BookOverview.tsx
import type { BookOverviewProps } from './BookOverview.types';

export const BookOverview = (props: BookOverviewProps) => {
  ...
};

// index.ts
export { BookOverview } from './BookOverview';
export type { BookOverviewProps } from './BookOverview.types';