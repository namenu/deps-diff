# deps-diff

두 개의 deps.edn 파일의 이행적인 의존성을 모두 비교합니다.

이게 왜 필요하냐면, deps 에 적어놓는 의존성을 비교하는 것 만으로는 부족하기 때문입니다.

물론 클로저 생태계는 semver를 따르지 않으며 (spec-ulation),
구체적인 잇점이 없다면 의존성을 업데이트 않는 것을 권장하기도 합니다.

그러나 필요에 의해 artifact 를 교체해야 할 때에는 여전히 주의해야 합니다.
특히 암묵적인 이행적 의존성이 변경될 경우 호환성 문제가 발생할 수 있기 때문입니다.

예를 들어, 아래와 같은 의존성 트리가 있다고 했을 때:

a -> b(1.0) -> c(1.0)
  -> d(1.0) -> c(1.0)

의존성 b를 업데이트 해야하는 상황이 생겼다고 가정합시다.

a -> b(2.0) -> c(2.0)
  -> d(1.0) -> c(1.0)

이런! b가 내부적으로 c를 2.0으로 업데이트 했군요?

우리가 특별히 c의 버전을 1.0으로 명시하지 않았다면, tools.deps/resolve-deps 는 c(2.0)을 가져올 것입니다.
deps selection 동작은 여기서 설명하고 있습니다.
https://clojure.org/reference/dep_expansion#_dep_selection

이 경우 우리가 d가 정상작동함을 보장할 방법은 없습니다.

하지만 이러한 잠재적인 위험이 있다는 것은 미리 감지할 수 있겠죠.
deps-diff 는 그러한 기능을 하기 위해 만들어진 GitHub Action 입니다.


### Inputs

- `base` - 변경 전의 deps.edn 에 해당하는 참조입니다. git ref 또는 파일 경로를 지정할 수 있습니다. 기본값은 PR의 base 브랜치에 해당하는 git ref 이며, 저장소 루트 경로의 `deps.edn`을 참조합니다.
`{{git-ref}}:{{path-to-deps.edn}}` 과 같이 지정할 수 있습니다. 
- `target` - 변경 후의 deps.edn 에 해당하는 참조입니다. git ref 또는 파일 경로를 지정할 수 있습니다. 기본값은 현재 경로의 `deps.edn`입니다.
- `format` - output의 형식을 결정합니다. `edn`, `markdown` 또는 `cli`를 지정할 수 있습니다. 기본값은 `edn` 입니다.
- `aliases` - basis 를 형성할 때 사용될 alias들을 지정합니다. quote 된 seq 로 표현되어야 합니다. (예: `'[:dev :test]'`)
기본값은 `nil`입니다.

| Name        | Description                                                                                                                                                               | Default Value              |
|-------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| `base`      | 참조하는 변경 전의 deps.edn입니다. git ref 또는 파일 경로를 지정할 수 있습니다. 기본값은 PR의 base 브랜치에 해당하는 git ref이며, 저장소 루트 경로의 `deps.edn`을 참조합니다. `{{git-ref}}:{{path-to-deps.edn}}`과 같이 지정할 수 있습니다. | PR의 base 브랜치의 git ref |
| `target`    | 참조하는 변경 후의 deps.edn입니다. git ref 또는 파일 경로를 지정할 수 있습니다. 기본값은 현재 경로의 `deps.edn`입니다.                                                                                          | 현재 경로의 `deps.edn`    |
| `format`    | output의 형식을 결정합니다. `edn`, `markdown`, 또는 `cli`를 지정할 수 있습니다. 기본값은 `edn` 입니다.                                                                                               | `edn`                      |
| `aliases`   | basis를 형성할 때 사용될 alias들을 지정합니다. quote된 seq로 표현되어야 합니다. (예: `'[:dev :test]'`) 기본값은 `nil`입니다.                                                                               | `nil`                      |



### Outputs

- `deps_diff` - 실행 결과가 출력되는 outlet 이름입니다. 워크플로우에서 action 의 id와 함께 사용하세요.

