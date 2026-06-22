from __future__ import annotations

from dataclasses import dataclass


DEFAULT_SERVICE_KEY_ENV = "DATA_GO_KR_SERVICE_KEY"


@dataclass(frozen=True)
class OperationSpec:
    operation_id: str
    path: str
    summary: str
    required_fields: tuple[str, ...]


@dataclass(frozen=True)
class SourceSpec:
    source_id: str
    agency: str
    dataset: str
    portal_url: str
    host: str
    required: bool
    service_key_env: str
    operations: tuple[OperationSpec, ...]

    def endpoint_for(self, operation: OperationSpec) -> str:
        return f"https://{self.host}{operation.path}"


MFDS_SOURCES: tuple[SourceSpec, ...] = (
    SourceSpec(
        source_id="mfds_drug_approval",
        agency="식품의약품안전처",
        dataset="의약품 제품 허가정보",
        portal_url="https://www.data.go.kr/data/15095677/openapi.do",
        host="apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService07",
        required=True,
        service_key_env=DEFAULT_SERVICE_KEY_ENV,
        operations=(
            OperationSpec(
                operation_id="getDrugPrdtPrmsnInq07",
                path="/getDrugPrdtPrmsnInq07",
                summary="의약품 제품 허가 목록",
                required_fields=("ITEM_SEQ", "ITEM_NAME", "ENTP_NAME"),
            ),
            OperationSpec(
                operation_id="getDrugPrdtPrmsnDtlInq06",
                path="/getDrugPrdtPrmsnDtlInq06",
                summary="의약품 제품 허가 상세정보",
                required_fields=("ITEM_SEQ", "ITEM_NAME", "ENTP_NAME"),
            ),
            OperationSpec(
                operation_id="getDrugPrdtMcpnDtlInq07",
                path="/getDrugPrdtMcpnDtlInq07",
                summary="의약품 제품 주성분 상세정보",
                required_fields=("ITEM_SEQ", "PRDUCT", "MTRAL_NM"),
            ),
        ),
    ),
    SourceSpec(
        source_id="mfds_easy_drug",
        agency="식품의약품안전처",
        dataset="의약품개요정보(e약은요)",
        portal_url="https://www.data.go.kr/data/15075057/openapi.do",
        host="apis.data.go.kr/1471000/DrbEasyDrugInfoService",
        required=False,
        service_key_env=DEFAULT_SERVICE_KEY_ENV,
        operations=(
            OperationSpec(
                operation_id="getDrbEasyDrugList",
                path="/getDrbEasyDrugList",
                summary="의약품개요정보 목록",
                required_fields=("itemSeq", "itemName", "entpName"),
            ),
        ),
    ),
    SourceSpec(
        source_id="mfds_dur_product",
        agency="식품의약품안전처",
        dataset="의약품안전사용서비스(DUR)품목정보",
        portal_url="https://www.data.go.kr/data/15059486/openapi.do",
        host="apis.data.go.kr/1471000/DURPrdlstInfoService03",
        required=True,
        service_key_env=DEFAULT_SERVICE_KEY_ENV,
        operations=(
            OperationSpec("getUsjntTabooInfoList03", "/getUsjntTabooInfoList03", "병용금기 정보조회", ("ITEM_SEQ", "ITEM_NAME", "TYPE_NAME")),
            OperationSpec("getOdsnAtentInfoList03", "/getOdsnAtentInfoList03", "노인주의 정보조회", ("ITEM_SEQ", "ITEM_NAME", "TYPE_NAME")),
            OperationSpec("getDurPrdlstInfoList03", "/getDurPrdlstInfoList03", "DUR품목정보 조회", ("ITEM_SEQ", "ITEM_NAME", "ENTP_NAME")),
            OperationSpec("getSpcifyAgrdeTabooInfoList03", "/getSpcifyAgrdeTabooInfoList03", "특정연령대금기 정보조회", ("ITEM_SEQ", "ITEM_NAME", "TYPE_NAME")),
            OperationSpec("getCpctyAtentInfoList03", "/getCpctyAtentInfoList03", "용량주의 정보조회", ("ITEM_SEQ", "ITEM_NAME", "TYPE_NAME")),
            OperationSpec("getMdctnPdAtentInfoList03", "/getMdctnPdAtentInfoList03", "투여기간주의 정보조회", ("ITEM_SEQ", "ITEM_NAME", "TYPE_NAME")),
            OperationSpec("getEfcyDplctInfoList03", "/getEfcyDplctInfoList03", "효능군중복 정보조회", ("ITEM_SEQ", "ITEM_NAME", "TYPE_NAME")),
            OperationSpec("getSeobangjeongPartitnAtentInfoList03", "/getSeobangjeongPartitnAtentInfoList03", "서방정분할주의 정보조회", ("ITEM_SEQ", "ITEM_NAME", "TYPE_NAME")),
            OperationSpec("getPwnmTabooInfoList03", "/getPwnmTabooInfoList03", "임부금기 정보조회", ("ITEM_SEQ", "ITEM_NAME", "TYPE_NAME")),
        ),
    ),
    SourceSpec(
        source_id="mfds_dur_ingredient",
        agency="식품의약품안전처",
        dataset="의약품안전사용서비스(DUR)성분정보",
        portal_url="https://www.data.go.kr/data/15056780/openapi.do",
        host="apis.data.go.kr/1471000/DURIrdntInfoService03",
        required=True,
        service_key_env=DEFAULT_SERVICE_KEY_ENV,
        operations=(
            OperationSpec("getUsjntTabooInfoList02", "/getUsjntTabooInfoList02", "병용금기 정보조회", ("INGR_CODE", "TYPE_NAME")),
            OperationSpec("getPwnmTabooInfoList02", "/getPwnmTabooInfoList02", "임부금기 정보조회", ("INGR_CODE", "TYPE_NAME")),
            OperationSpec("getCpctyAtentInfoList02", "/getCpctyAtentInfoList02", "용량주의 정보조회", ("INGR_CODE", "TYPE_NAME")),
            OperationSpec("getMdctnPdAtentInfoList02", "/getMdctnPdAtentInfoList02", "투여기간주의 정보조회", ("INGR_CODE", "TYPE_NAME")),
            OperationSpec("getOdsnAtentInfoList02", "/getOdsnAtentInfoList02", "노인주의 정보조회", ("INGR_CODE", "TYPE_NAME")),
            OperationSpec("getSpcifyAgrdeTabooInfoList02", "/getSpcifyAgrdeTabooInfoList02", "특정연령대금기 정보조회", ("INGR_CODE", "TYPE_NAME")),
            OperationSpec("getEfcyDplctInfoList02", "/getEfcyDplctInfoList02", "효능군중복 정보조회", ("INGR_CODE", "TYPE_NAME")),
        ),
    ),
)


def list_sources() -> tuple[SourceSpec, ...]:
    return MFDS_SOURCES


def find_source(source_id: str) -> SourceSpec:
    for source in MFDS_SOURCES:
        if source.source_id == source_id:
            return source
    raise KeyError(f"Unknown source_id: {source_id}")


def find_operation(source: SourceSpec, operation_id: str) -> OperationSpec:
    for operation in source.operations:
        if operation.operation_id == operation_id:
            return operation
    raise KeyError(f"Unknown operation_id for {source.source_id}: {operation_id}")
