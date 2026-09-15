import os
import re

PLATFORM_DIR = "/Users/humolix/Documents/Dev/Volur-Lite/src/volur-lite-platform"

REPLACEMENTS = [
    # volur-challenge
    ("com.humolix.volur.challenge.domain.entity.stage.", "com.humolix.volur.challenge.domain.entity."),
    ("com.humolix.volur.challenge.domain.dto.stage.", "com.humolix.volur.challenge.domain.dto."),
    ("com.humolix.volur.challenge.domain.vo.stage.", "com.humolix.volur.challenge.domain.vo."),
    ("com.humolix.volur.challenge.repository.stage.", "com.humolix.volur.challenge.repository."),
    ("com.humolix.volur.challenge.service.admin.stage.", "com.humolix.volur.challenge.service.admin."),
    ("com.humolix.volur.challenge.controller.v1.admin.stage.", "com.humolix.volur.challenge.controller.v1.admin."),
    ("ChallengeStageType", "ChallengeMode"),
    # volur-competition
    ("com.humolix.volur.competition.domain.dto.stage.", "com.humolix.volur.competition.domain.dto."),
    ("com.humolix.volur.competition.domain.entity.stage.", "com.humolix.volur.competition.domain.entity."),
    ("com.humolix.volur.competition.domain.vo.stage.", "com.humolix.volur.competition.domain.vo."),
    ("com.humolix.volur.competition.repository.stage.", "com.humolix.volur.competition.repository."),
    ("com.humolix.volur.competition.service.stage.", "com.humolix.volur.competition.service."),
    ("com.humolix.volur.competition.controller.v1.stage.", "com.humolix.volur.competition.controller.v1."),
    ("com.humolix.volur.competition.controller.v1.admin.stage.", "com.humolix.volur.competition.controller.v1.admin.ctf."),
    ("com.humolix.volur.competition.service.admin.stage.", "com.humolix.volur.competition.service.admin."),
    ("StageStatus", "AwdRoundStatus"),
    ("CtfStageScoreboardVO", "CtfScoreboardVO"),
    ("CtfStageService", "CtfCompetitionService"),
    ("AwdStageService", "AwdCompetitionService"),
    ("AwdPlusStageService", "AwdPlusCompetitionService"),
    ("IswStageService", "IswCompetitionService"),
    ("TheoryStageService", "TheoryCompetitionService"),
    ("ctfStageService", "ctfCompetitionService"),
    ("awdStageService", "awdCompetitionService"),
    ("awdPlusStageService", "awdPlusCompetitionService"),
    ("iswStageService", "iswCompetitionService"),
    ("theoryStageService", "theoryCompetitionService"),
    ("getCtfStageScoreboard", "getCtfScoreboard"),
    ("com.humolix.volur.competition.annotation.RequiredCompetitionStageRunning", "com.humolix.volur.competition.annotation.RequiredCompetitionRunning"),
    ("RequiredCompetitionStageRunning", "RequiredCompetitionRunning"),
]

changed_count = 0
for root, dirs, files in os.walk(PLATFORM_DIR):
    for f in files:
        if not f.endswith(".java") and not f.endswith(".xml"):
            continue
        filepath = os.path.join(root, f)
        with open(filepath, 'r', encoding='utf-8') as rf:
            original = rf.read()
        content = original
        for old, new in REPLACEMENTS:
            content = content.replace(old, new)
        # Specific annotation normalization: @RequiredCompetitionRunning(type = CompetitionType.XYZ) -> @RequiredCompetitionRunning(CompetitionType.XYZ)
        content = re.sub(r'@RequiredCompetitionRunning\(type\s*=\s*(CompetitionType\.[A-Z_]+)\)', r'@RequiredCompetitionRunning(\1)', content)
        # Challenge.java and subclasses: getStageType() -> getMode()
        content = re.sub(r'public\s+ChallengeMode\s+getStageType\(\)', 'public ChallengeMode getMode()', content)
        content = re.sub(r'ChallengeMode\s+getStageType\(\);', 'ChallengeMode getMode();', content)
        if content != original:
            with open(filepath, 'w', encoding='utf-8') as wf:
                wf.write(content)
            changed_count += 1

print(f"Updated references in {changed_count} files.")
