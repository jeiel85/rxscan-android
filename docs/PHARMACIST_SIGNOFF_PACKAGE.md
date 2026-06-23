# Pharmacist Sign-off Package (Goal 08)

All user-facing **fixed medical copy** that ships in the app, collated for
pharmacist review and sign-off (`release_gate: pharmacist_signoff`; AGENTS.md
medical copy rule). Copy is fixed in code — never generated. Source of truth is
cited so the reviewer signs off the exact shipping strings.

## Confidence wording (`feature:review` `ReviewCopy.confidenceLabel`)

- VERIFIED_IDENTIFIER → "코드가 일치했습니다"
- HIGH_CONFIDENCE_REVIEW → "이름·함량·제형이 일치합니다. 직접 확인해 주세요"
- AMBIGUOUS → "비슷한 약이 여러 개입니다"
- UNRESOLVED → "정확한 약을 확인하지 못했습니다"
- Edit hint → "흐리거나 잘린 글자는 직접 확인해 주세요. 앱은 보이지 않는 내용을 추측하지 않습니다."

Banned (asserted absent): "AI가 확정함", "안전합니다", "100% 일치", numeric percentages.

## DUR safety wording (`core:model` `DurWording`)

- Title → "공식 안전사용 정보가 있습니다"
- Body(type) → "확인한 약들 사이에서 {유형} 정보가 조회되었습니다. 처방에는 의료진의 판단에 따른
  사유가 있을 수 있으므로 약을 임의로 중단하거나 변경하지 말고, 처방한 의료기관 또는 조제약국에
  확인하세요."
- No findings → "확인한 약들에서 조회된 공식 병용·중복 안전사용 정보가 없습니다. 이는 안전함을
  보장하지 않으며, 자세한 내용은 약사 또는 의료진에게 확인하세요."
- Insufficient → "성분 정보를 확인할 수 없는 약이 있어 안전사용 정보를 완전히 평가하지 못했습니다.
  이는 상호작용이 없다는 의미가 아닙니다. 약사 또는 의료진에게 확인하세요."
- Stale → "현재 데이터로는 최신 DUR 확인을 제공하지 않습니다."

Banned (asserted absent): unconditional stop/change/"두 배"/"함께 복용하지 마" instructions.

## Official-info states (`feature:drugdetail` `DetailCopy`)

- Missing easy-info → "이 제품은 현재 앱이 사용하는 소비자용 공식 설명 데이터에 내용이 없습니다.
  아래에는 확인 가능한 허가정보 원문만 표시합니다."
- Stale/warning banner → "공식 정보 데이터가 {N}일 동안 업데이트되지 않았습니다. 표시된 기준일을
  확인하고 최신 정보는 약사 또는 공식 원문에서 확인하세요."
- Revoked banner → "이 데이터 버전은 더 이상 사용할 수 없습니다. 앱의 공식 데이터를 업데이트해 주세요."

## Capture/scan guidance (`core:model` `ScanError`)

Blur/glare/text-size/clipping/insufficient/parser-conflict/no-medication messages —
all actionable rescan guidance, never a medicine guess.

## Sign-off

- [ ] Pharmacist reviewed all strings above against the shipping build.
- [ ] No string implies diagnosis, dosing, or an unconditional stop/change.
- [ ] Photographed direction vs official information separation confirmed.
- Reviewer / date / build version: ____________________
