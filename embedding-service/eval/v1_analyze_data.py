"""Q&A 데이터 분석 (docs/DATA_ANALYSIS.md 재현용).

python3 eval/analyze_data.py  → 유형(여행사/항공사)별 주제 분포, 기관별 출처, 주제 분류표를 출력한다.
표준 라이브러리만 사용한다.
"""
import json, re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
rows = [json.loads(l) for l in open(ROOT / "data/v1/qa_embeddings.jsonl", encoding="utf-8")]

# 질문 문장 키워드 규칙 (위에서부터 우선 적용)
RULES = [
    ("비정상 상황 (결항·지연·분실·현지문제)", r"결항|지연|비운항|분실|파손|문제가 생기|최소 출발 인원"),
    ("취소·환불", r"취소|환불|위약|수수료안심"),
    ("변경 (일정·이름·결제수단)", r"변경|수정|바꿀"),
    ("수하물·반입 제한", r"수하물|짐|보조배터리|위탁할 수 없는"),
    ("체크인·탑승", r"체크인은|체크인\(|체크인 |체크인을|수속|탑승은|키오스크|탑승권|도심공항"),
    ("특수 고객 (유아·반려동물·임산부)", r"유아|아기|반려동물|소아|24개월|임산부|나이 구분"),
    ("예약·구매·발권", r"예약|구매|발권|e-티켓|공동운항|여권"),
    ("고객센터·운영시간", r"고객센터|전화번호|상담시간"),
    ("마일리지·결제·영수증", r"마일리지|결제|영수증|서비스 수수료"),
    ("부가서비스 (호텔·보험·기내식)", r"기내식|보험|호텔"),
]
# 규칙으로 애매한 5건은 수동 판정
OVERRIDE = {
    "054": "특수 고객 (유아·반려동물·임산부)",  # 유아 동반 수하물
    "067": "특수 고객 (유아·반려동물·임산부)",  # 유아 동반 체크인 제한
    "075": "부가서비스 (호텔·보험·기내식)",     # 사전 구매 수하물 환불
    "076": "부가서비스 (호텔·보험·기내식)",     # 사전 기내식
    "041": "마일리지·결제·영수증",              # 스카이패스 회원번호
}
OFFICIAL = {"하나투어": "hanatour.com", "모두투어": "modetour.co.kr", "노랑풍선": "ybtour.co.kr",
            "참좋은여행": "verygoodtour.com", "대한항공": "koreanair.com", "제주항공": "jejuair.net",
            "에어로케이": "aerok.com"}
NUM = re.compile(r"\d[\d,]*\s?(원|만원|kg|%|시간|영업일|일|주|분|개|cm|Wh|mAh|세|개월|명|마리)|\d{1,2}:\d{2}|\d+~\d+")


def topic(r):
    if r["id"] in OVERRIDE:
        return OVERRIDE[r["id"]]
    for name, pat in RULES:
        if re.search(pat, r["question"]):
            return name
    return "기타"


for r in rows:
    r["topic"] = topic(r)
    r["official"] = OFFICIAL[r["org"]] in r["source"]

if __name__ == "__main__":
    for t in ("여행사", "항공사"):
        R = [r for r in rows if r["org_type"] == t]
        print(f"\n## {t}: {len(R)}건")
        for k, v in Counter(r["topic"] for r in R).most_common():
            print(f"  {v:2d} ({v / len(R) * 100:4.1f}%) {k}")
        for o in dict.fromkeys(r["org"] for r in R):
            Ro = [r for r in R if r["org"] == o]
            print(f"  {o}: {len(Ro)}건, 공식 출처 {sum(r['official'] for r in Ro)}/{len(Ro)}")
        print(f"  공식 출처 {sum(r['official'] for r in R)}/{len(R)}, 구체 수치 포함 {sum(bool(NUM.search(r['answer'])) for r in R)}/{len(R)}, "
              f"평균 답변 {sum(len(r['answer']) for r in R) / len(R):.0f}자")
    print("\n## 분류표")
    for r in rows:
        print(f"{r['id']}\t{r['org_type']}\t{r['org']}\t{r['topic']}\t{'공식' if r['official'] else '비공식'}\t{r['question']}")
