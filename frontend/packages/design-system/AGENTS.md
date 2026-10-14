# Figma 기반 디자인 시스템 컴포넌트 스타일 pivot 가이드

## 목적
Figma 디자인을 참고하여 기존 디자인 시스템 컴포넌트의 스타일을 수정한다.
기존 컴포넌트의 인터페이스와 동작을 유지하면서 디자인 변경에 필요한 최소한의 코드만 수정하는 것을 원칙으로 한다.

## 적용 범위

- Figma를 참고하여 기존 컴포넌트의 기본 스타일 또는 특정 Props의 스타일을 변경하는 작업에 적용한다.
- 수정할 컴포넌트와 Props 등 구체적인 작업 대상은 개별 프롬프트에서 지정한다.
- 새로운 컴포넌트 개발이나 기능 변경 작업에는 적용하지 않는다.

1. 작업 범위

- 지정된 컴포넌트의 스타일만 수정한다.
- 작업 대상과 범위는 개별 프롬프트의 지시를 따른다.
- 작업과 무관한 코드 수정이나 리팩터링을 하지 않는다.
- Figma에서 확인할 수 없는 디자인 속성은 기존 스타일을 유지한다.

2. 기존 코드 유지

- 기존 컴포넌트의 Props, 구조, 동작을 변경하지 않는다.
- 새로운 Props나 기능을 임의로 추가하지 않는다.

3. CSS 작성

3-1. CSS 작성 규칙

- Props별 스타일 책임을 분리한다.
- 각 Props에 따른 스타일은 해당 Props의 CSS 규칙에서 독립적으로 관리한다.
- Props 조합별 CSS 선택자나 중복 규칙을 추가하지 않는다.
- Props 조합별 스타일이 필요하다고 판단되는 경우 임의로 구현하지 말고, 필요한 이유와 해당 스타일을 보고한다.

3-2. CSS 디자인 토큰
핵심 원칙: 디자인 토큰을 우선 사용하되, 적절한 토큰이 없으면 Figma의 원시값을 사용한다.

사용 가능한 토큰 파일:
- ./frontend/packages/design-system/src/styles/variables/tokens/semantic.css
- ./frontend/packages/design-system/src/styles/variables/tokens/primitive.css

토큰 선택 우선순위:

1. 디자인 의도와 값이 모두 일치하는 semantic 토큰
2. 값이 일치하는 primitive 토큰
3. 적절한 토큰이 없는 경우 Figma의 원시값(색상, 크기, 간격, radius 등)


Figma 값과 다른 토큰을 억지로 적용하지 않는다. 토큰을 새로 만들거나 기존 토큰의 정의를 변경하지 않는다.

다음 파일의 토큰은 사용하지 마.
- ./frontend/packages/design-system/src/styles/variables/tokens/layout.css
- ./frontend/packages/design-system/src/styles/variables/tokens/loading.css
- ./frontend/packages/design-system/src/styles/variables/tokens/typography.css

적절한 토큰이 없다는 이유만으로 Figma 스타일 반영을 생략하거나 확인 요청을 남기지 않는다.

다만 Figma에서 값을 확인할 수 없거나 디자인 의도가 모호한 경우에는 기존 스타일을 유지하고 보고한다.

4. Storybook

Storybook 수정이 요청된 경우에만 적용한다.

- 변경된 디자인을 확인할 수 있도록 기존 Story를 수정한다.
- Figma의 텍스트 콘텐츠는 재현하지 않아도 된다.
- 기존 텍스트를 유지하거나 Button처럼 일반적인 텍스트를 사용한다.
- 새로운 Props나 Story를 불필요하게 추가하지 않는다.

5. 검증 
- 대상 컴포넌트와 직접 수정한 파일의 타입 오류만 확인한다.
- 컴포넌트를 사용하는 전체 애플리케이션에 대한 타입 검사는 요구하지 않는다.
- 패키지 전체 타입 검사에서 기존 오류가 발견되더라도 작업 범위 밖의 코드는 수정하지 않는다.

6. 작업 완료 보고

다음 사항만 간결하게 보고한다.

- 변경 사항
- 검증 결과
- 확인이 필요한 부분
