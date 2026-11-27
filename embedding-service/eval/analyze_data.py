"""v2 문답 데이터 분석 (docs/DATA_ANALYSIS.md 재현용). 표준 라이브러리만 사용.

python3 eval/analyze_data.py → 유형(항공사/여행사/숙박업체)별 주제·출처·수치·질문 변형 통계와 분류표
"""
import json, re
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
rows = [json.loads(l) for l in open(ROOT / "data/qa_embeddings.jsonl", encoding="utf-8")]
parents = [r for r in rows if r["variant"] == "original"]
variants = defaultdict(dict)
for r in rows:
    variants[r["parent_id"]][r["variant"]] = r["question"]
TYPES = ["항공사", "여행사", "숙박업체"]

# 질문 문장 키워드 규칙 (위에서부터 우선 적용). 원문 질문 + 다른 표현 변형을 함께 본다.
RULES = [
    ("비정상 상황 (결항·지연·분실·현지문제)", r"결항|지연|비운항|분실|파손|문제가 생기|최소 출발 인원|기상악화|천재지변|감염병|분쟁|크게 바꾸|취소하면\?$|취소하거나 크게"),
    ("고객센터·문의", r"고객센터|전화번호|상담시간|문의|누구에게"),
    ("수하물·반입 제한", r"수하물|짐|보조배터리|위탁할 수 없는"),
    ("취소·환불", r"취소|환불|위약|수수료안심|되돌릴|복구|복원"),
    ("변경 (일정·이름·결제수단)", r"변경|수정|바꿀|바꾸"),
    ("체크인·탑승", r"체크인|수속|탑승은|키오스크|탑승권|도심공항"),
    ("특수 고객 (유아·반려동물·임산부)", r"유아|아기|반려동물|소아|24개월|임산부|나이 구분"),
    ("예약·구매·발권", r"예약|구매|발권|e-티켓|공동운항|여권|확인서|운임"),
    ("마일리지·결제·영수증", r"마일리지|결제|영수증|서비스 수수료|마일"),
    ("부가서비스 (호텔·보험·기내식)", r"기내식|보험|호텔"),
]
OVERRIDE = {  # 규칙으로 애매한 원문은 수동 판정
    "항공-016": "특수 고객 (유아·반려동물·임산부)", "항공-029": "특수 고객 (유아·반려동물·임산부)",
    "항공-037": "부가서비스 (호텔·보험·기내식)", "항공-038": "부가서비스 (호텔·보험·기내식)",
    "항공-003": "마일리지·결제·영수증", "항공-085": "수하물·반입 제한", "항공-086": "수하물·반입 제한",
    "항공-050": "취소·환불", "숙박-005": "취소·환불", "숙박-030": "취소·환불",
    "여행-024": "부가서비스 (호텔·보험·기내식)",  # 호텔 늦은 체크인
}
OFFICIAL = {"하나투어": ["hanatour.com"], "모두투어": ["modetour.co.kr"], "노랑풍선": ["ybtour.co.kr"],
            "참좋은여행": ["verygoodtour.com"], "대한항공": ["koreanair.com"], "제주항공": ["jejuair.net"],
            "에어로케이": ["aerok.com"], "델타항공": ["delta.com"], "유나이티드항공": ["united.com"],
            "에미레이트항공": ["emirates.com"], "싱가포르항공": ["singaporeair.com"], "캐세이퍼시픽": ["cathaypacific.com"],
            "루프트한자": ["lufthansa.com"], "ANA(전일본공수)": ["ana.co.jp"], "아고다": ["agoda.com"],
            "호텔스컴바인": ["hotelscombined.co.kr", "hotelscombined.com"], "트립닷컴": ["trip.com"],
            "부킹닷컴": ["booking.com"], "야놀자": ["yanolja.com"], "여기어때": ["yeogi.com", "goodchoice.kr"]}
DOMESTIC_AIR = {"대한항공", "제주항공", "에어로케이"}
NUM = re.compile(r"\d[\d,]*\s?(원|만원|kg|%|시간|영업일|일|주|분|개|cm|Wh|mAh|세|개월|명|마리)|\d{1,2}:\d{2}|\d+~\d+")
UNCERTAIN = re.compile(r"정리글|정리가 있|설명이 있|안내하는 글|확인되지 않|확정할 수 없|글마다|자료마다|오래된 글|공지 기준")


def host(url):
    return re.sub(r"^https?://([^/:]+).*", r"\1", url).lower()


def topic(p):
    if p["parent_id"] in OVERRIDE:
        return OVERRIDE[p["parent_id"]]
    text = p["original_question"] + " " + variants[p["parent_id"]].get("rephrase", "")
    for name, pat in RULES:
        if re.search(pat, text):
            return name
    return "기타"


for p in parents:
    p["topic"] = topic(p)
    p["official"] = any(host(p["source"]) == d or host(p["source"]).endswith("." + d) for d in OFFICIAL[p["org"]])
    p["uncertain"] = bool(UNCERTAIN.search(p["answer"]))

if __name__ == "__main__":
    print(f"원문 {len(parents)}건, 질문 변형 포함 {len(rows)}행, 기관 {len({p['org'] for p in parents})}곳")
    for t in TYPES:
        P = [p for p in parents if p["category"] == t]
        print(f"\n## {t}: {len(P)}건")
        for k, v in Counter(p["topic"] for p in P).most_common():
            print(f"  {v:2d} ({v / len(P) * 100:4.1f}%) {k}")
        for o in dict.fromkeys(p["org"] for p in P):
            Po = [p for p in P if p["org"] == o]
            top = Counter(p["topic"] for p in Po).most_common(1)[0]
            print(f"  {o}: {len(Po)}건, 공식 출처 {sum(p['official'] for p in Po)}/{len(Po)}, 최다 주제 {top[0]} {top[1]}")
        print(f"  공식 출처 {sum(p['official'] for p in P)}/{len(P)}, 구체 수치 {sum(bool(NUM.search(p['answer'])) for p in P)}/{len(P)}, "
              f"불확실 표현 {sum(p['uncertain'] for p in P)}/{len(P)}, 평균 답변 {sum(len(p['answer']) for p in P) / len(P):.0f}자")
    air = [p for p in parents if p["category"] == "항공사"]
    for name, group in (("국내 항공사", [p for p in air if p["org"] in DOMESTIC_AIR]),
                        ("해외 항공사", [p for p in air if p["org"] not in DOMESTIC_AIR])):
        cr = sum(p["topic"] == "취소·환불" for p in group)
        print(f"\n{name}: {len(group)}건, 취소·환불 {cr}건({cr / len(group) * 100:.0f}%), "
              f"공식 출처 {sum(p['official'] for p in group)}/{len(group)}, 불확실 표현 {sum(p['uncertain'] for p in group)}")

    print("\n## 질문 변형")
    for v in ("original", "casual", "keyword", "rephrase"):
        qs = [r["question"] for r in rows if r["variant"] == v]
        org_in = sum(1 for r in rows if r["variant"] == v and r["org"].split("(")[0][:2] in r["question"])
        print(f"  {v:9s} 평균 {sum(len(q) for q in qs) / len(qs):4.1f}자, 기관명 포함 {org_in}/{len(qs)}")
    dup = Counter(p["original_question"] for p in parents)
    print("  기관 간 같은 원문 질문:", {q: n for q, n in dup.items() if n > 1})

    print("\n## 분류표")
    for p in parents:
        print(f"{p['parent_id']}\t{p['category']}\t{p['org']}\t{p['topic']}\t{'공식' if p['official'] else '비공식'}\t{p['original_question']}")
