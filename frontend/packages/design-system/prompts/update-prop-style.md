
Figma 디자인을 참고하여 디자인 시스템의 스타일을 수정해 줘.

## 작업 대상

- 컴포넌트: frontend/packages/design-system/src/components/컴포넌트 이름 
- 대상 Props: a={a1}
- Figma: [Figma 링크]

## 작업 요청

- frontend/packages/design-system/AGENTS.md에 정의된 작업 원칙을 따라줘.
- 단 이번 작업은 기본 스타일이 아닌 특정 Props의 스타일 변경을 대상으로 한다.
- 대상 prop에 해당하는 디자인만 변경한다.
- 대상 Props의 기본값은 기존 컴포넌트 코드에서 확인하며, 기본값에 해당하는 스타일은 유지한다.
- 다른 Props가 담당하는 스타일과 동작은 변경하지 않는다.
- 대상 Props의 스타일은 다른 Props의 값에 의존하지 않도록 독립적으로 작성한다. Props 조합별 CSS 선택자나 중복 규칙을 추가하지 않는다.
- Figma 디자인을 구현하려면 Props 조합별 스타일이 필요하다고 판단되는 경우에도 임의로 구현하지 말고, 필요한 이유와 해당 스타일을 보고한다.

## Storybook (필요한 경우에만 포함)
- 변경된 디자인을 확인할 수 있도록 기존 Storybook도 수정해 줘.
