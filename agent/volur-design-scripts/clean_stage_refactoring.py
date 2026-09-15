import os
import shutil
import re

BASE_DIR = "/Users/humolix/Documents/Dev/Volur-Lite"
CHALLENGE_JAVA = os.path.join(BASE_DIR, "src/volur-lite-platform/volur-challenge/src/main/java/com/humolix/volur/challenge")
COMPETITION_JAVA = os.path.join(BASE_DIR, "src/volur-lite-platform/volur-competition/src/main/java/com/humolix/volur/competition")

def move_and_replace_package(src_dir, dst_dir, old_pkg_part, new_pkg_part):
    if not os.path.exists(src_dir):
        return
    for root, dirs, files in os.walk(src_dir):
        for f in files:
            if not f.endswith(".java"):
                continue
            src_file = os.path.join(root, f)
            rel_path = os.path.relpath(src_file, src_dir)
            dst_file = os.path.join(dst_dir, rel_path)
            os.makedirs(os.path.dirname(dst_file), exist_ok=True)
            with open(src_file, 'r', encoding='utf-8') as rf:
                content = rf.read()
            content = content.replace(old_pkg_part, new_pkg_part)
            with open(dst_file, 'w', encoding='utf-8') as wf:
                wf.write(content)
            os.remove(src_file)

# 1. volur-challenge moves
challenge_mappings = [
    ("controller/v1/admin/stage", "controller/v1/admin", "challenge.controller.v1.admin.stage", "challenge.controller.v1.admin"),
    ("domain/dto/stage", "domain/dto", "challenge.domain.dto.stage", "challenge.domain.dto"),
    ("domain/entity/stage", "domain/entity", "challenge.domain.entity.stage", "challenge.domain.entity"),
    ("domain/vo/stage", "domain/vo", "challenge.domain.vo.stage", "challenge.domain.vo"),
    ("repository/stage", "repository", "challenge.repository.stage", "challenge.repository"),
    ("service/admin/stage", "service/admin", "challenge.service.admin.stage", "challenge.service.admin"),
]

for src_sub, dst_sub, old_p, new_p in challenge_mappings:
    s = os.path.join(CHALLENGE_JAVA, src_sub)
    d = os.path.join(CHALLENGE_JAVA, dst_sub)
    move_and_replace_package(s, d, old_p, new_p)
    if os.path.exists(s):
        shutil.rmtree(s)

# ChallengeStageType -> ChallengeMode
cst_file = os.path.join(CHALLENGE_JAVA, "domain/enums/ChallengeStageType.java")
cm_file = os.path.join(CHALLENGE_JAVA, "domain/enums/ChallengeMode.java")
if os.path.exists(cst_file):
    with open(cst_file, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace("ChallengeStageType", "ChallengeMode")
    with open(cm_file, 'w', encoding='utf-8') as f:
        f.write(c)
    os.remove(cst_file)

# 2. volur-competition moves
competition_mappings = [
    ("domain/dto/stage", "domain/dto", "competition.domain.dto.stage", "competition.domain.dto"),
    ("domain/entity/stage", "domain/entity", "competition.domain.entity.stage", "competition.domain.entity"),
    ("domain/vo/stage", "domain/vo", "competition.domain.vo.stage", "competition.domain.vo"),
    ("repository/stage", "repository", "competition.repository.stage", "competition.repository"),
]

for src_sub, dst_sub, old_p, new_p in competition_mappings:
    s = os.path.join(COMPETITION_JAVA, src_sub)
    d = os.path.join(COMPETITION_JAVA, dst_sub)
    move_and_replace_package(s, d, old_p, new_p)
    if os.path.exists(s):
        shutil.rmtree(s)

# StageStatus -> AwdRoundStatus
ss_file = os.path.join(COMPETITION_JAVA, "domain/enums/StageStatus.java")
ars_file = os.path.join(COMPETITION_JAVA, "domain/enums/AwdRoundStatus.java")
if os.path.exists(ss_file):
    with open(ss_file, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace("StageStatus", "AwdRoundStatus")
    with open(ars_file, 'w', encoding='utf-8') as f:
        f.write(c)
    os.remove(ss_file)

# CtfStageScoreboardVO -> CtfScoreboardVO
css_file = os.path.join(COMPETITION_JAVA, "domain/vo/scoreboard/CtfStageScoreboardVO.java")
cs_file = os.path.join(COMPETITION_JAVA, "domain/vo/scoreboard/CtfScoreboardVO.java")
if os.path.exists(css_file):
    with open(css_file, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace("CtfStageScoreboardVO", "CtfScoreboardVO")
    with open(cs_file, 'w', encoding='utf-8') as f:
        f.write(c)
    os.remove(css_file)

# AdminCtfMountController
acm_old = os.path.join(COMPETITION_JAVA, "controller/v1/admin/stage/AdminCtfMountController.java")
acm_new = os.path.join(COMPETITION_JAVA, "controller/v1/admin/ctf/AdminCtfMountController.java")
if os.path.exists(acm_old):
    os.makedirs(os.path.dirname(acm_new), exist_ok=True)
    with open(acm_old, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace("package com.humolix.volur.competition.controller.v1.admin.stage;", "package com.humolix.volur.competition.controller.v1.admin.ctf;")
    c = c.replace('"/api/v1/admin/competitions/{competitionId}/stage/ctf/mount"', '"/api/v1/admin/competitions/{competitionId}/ctf/mount"')
    with open(acm_new, 'w', encoding='utf-8') as f:
        f.write(c)
    os.remove(acm_old)
    shutil.rmtree(os.path.dirname(acm_old))

# AdminCtfMountService & Impl
ams_old = os.path.join(COMPETITION_JAVA, "service/admin/stage/AdminCtfMountService.java")
ams_new = os.path.join(COMPETITION_JAVA, "service/admin/ctf/AdminCtfMountService.java")
ams_impl_old = os.path.join(COMPETITION_JAVA, "service/admin/stage/impl/AdminCtfMountServiceImpl.java")
ams_impl_new = os.path.join(COMPETITION_JAVA, "service/admin/ctf/impl/AdminCtfMountServiceImpl.java")

if os.path.exists(ams_old):
    os.makedirs(os.path.dirname(ams_new), exist_ok=True)
    with open(ams_old, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace("package com.humolix.volur.competition.service.admin.stage;", "package com.humolix.volur.competition.service.admin.ctf;")
    with open(ams_new, 'w', encoding='utf-8') as f:
        f.write(c)
    os.remove(ams_old)

if os.path.exists(ams_impl_old):
    os.makedirs(os.path.dirname(ams_impl_new), exist_ok=True)
    with open(ams_impl_old, 'r', encoding='utf-8') as f:
        c = f.read()
    c = c.replace("package com.humolix.volur.competition.service.admin.stage.impl;", "package com.humolix.volur.competition.service.admin.ctf.impl;")
    c = c.replace("com.humolix.volur.competition.service.admin.stage.AdminCtfMountService", "com.humolix.volur.competition.service.admin.ctf.AdminCtfMountService")
    with open(ams_impl_new, 'w', encoding='utf-8') as f:
        f.write(c)
    os.remove(ams_impl_old)
    if os.path.exists(os.path.join(COMPETITION_JAVA, "service/admin/stage")):
        shutil.rmtree(os.path.join(COMPETITION_JAVA, "service/admin/stage"))

# Competition arena controllers & services
# Rename CtfStageController -> CtfCompetitionController in controller/v1/ctf
ctrl_dir = os.path.join(COMPETITION_JAVA, "controller/v1/stage")
if os.path.exists(ctrl_dir):
    # ctf
    cf = os.path.join(ctrl_dir, "CtfStageController.java")
    if os.path.exists(cf):
        dst = os.path.join(COMPETITION_JAVA, "controller/v1/ctf/CtfCompetitionController.java")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(cf, 'r', encoding='utf-8') as f:
            c = f.read()
        c = c.replace("package com.humolix.volur.competition.controller.v1.stage;", "package com.humolix.volur.competition.controller.v1.ctf;")
        c = c.replace('"/api/v1/competitions/{competitionId}/stage/ctf"', '"/api/v1/competitions/{competitionId}/ctf"')
        c = c.replace("public class CtfStageController", "public class CtfCompetitionController")
        c = c.replace("CtfStageService", "CtfCompetitionService")
        c = c.replace("ctfStageService", "ctfCompetitionService")
        # Remove stageId param from listChallenges
        c = re.sub(r'@RequestParam\(required\s*=\s*false\)\s*Long\s*stageId,\s*', '', c)
        c = c.replace("ctfCompetitionService.listChallenges(competitionId, stageId, userId)", "ctfCompetitionService.listChallenges(competitionId, userId)")
        with open(dst, 'w', encoding='utf-8') as f:
            f.write(c)
        os.remove(cf)
    # awd
    af = os.path.join(ctrl_dir, "AwdStageController.java")
    if os.path.exists(af):
        dst = os.path.join(COMPETITION_JAVA, "controller/v1/awd/AwdCompetitionController.java")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(af, 'r', encoding='utf-8') as f:
            c = f.read()
        c = c.replace("package com.humolix.volur.competition.controller.v1.stage;", "package com.humolix.volur.competition.controller.v1.awd;")
        c = c.replace('"/api/v1/competitions/{competitionId}/stage/awd"', '"/api/v1/competitions/{competitionId}/awd"')
        c = c.replace("public class AwdStageController", "public class AwdCompetitionController")
        c = c.replace("AwdStageService", "AwdCompetitionService")
        c = c.replace("awdStageService", "awdCompetitionService")
        with open(dst, 'w', encoding='utf-8') as f:
            f.write(c)
        os.remove(af)
    # awdp
    apf = os.path.join(ctrl_dir, "AwdPlusStageController.java")
    if os.path.exists(apf):
        dst = os.path.join(COMPETITION_JAVA, "controller/v1/awdp/AwdPlusCompetitionController.java")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(apf, 'r', encoding='utf-8') as f:
            c = f.read()
        c = c.replace("package com.humolix.volur.competition.controller.v1.stage;", "package com.humolix.volur.competition.controller.v1.awdp;")
        c = c.replace('"/api/v1/competitions/{competitionId}/stage/awdp"', '"/api/v1/competitions/{competitionId}/awdp"')
        c = c.replace("public class AwdPlusStageController", "public class AwdPlusCompetitionController")
        c = c.replace("AwdPlusStageService", "AwdPlusCompetitionService")
        c = c.replace("awdPlusStageService", "awdPlusCompetitionService")
        with open(dst, 'w', encoding='utf-8') as f:
            f.write(c)
        os.remove(apf)
    # isw
    isf = os.path.join(ctrl_dir, "IswStageController.java")
    if os.path.exists(isf):
        dst = os.path.join(COMPETITION_JAVA, "controller/v1/isw/IswCompetitionController.java")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(isf, 'r', encoding='utf-8') as f:
            c = f.read()
        c = c.replace("package com.humolix.volur.competition.controller.v1.stage;", "package com.humolix.volur.competition.controller.v1.isw;")
        c = c.replace('"/api/v1/competitions/{competitionId}/stage/isw"', '"/api/v1/competitions/{competitionId}/isw"')
        c = c.replace("public class IswStageController", "public class IswCompetitionController")
        c = c.replace("IswStageService", "IswCompetitionService")
        c = c.replace("iswStageService", "iswCompetitionService")
        with open(dst, 'w', encoding='utf-8') as f:
            f.write(c)
        os.remove(isf)
    # theory
    thf = os.path.join(ctrl_dir, "TheoryStageController.java")
    if os.path.exists(thf):
        dst = os.path.join(COMPETITION_JAVA, "controller/v1/theory/TheoryCompetitionController.java")
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        with open(thf, 'r', encoding='utf-8') as f:
            c = f.read()
        c = c.replace("package com.humolix.volur.competition.controller.v1.stage;", "package com.humolix.volur.competition.controller.v1.theory;")
        c = c.replace('"/api/v1/competitions/{competitionId}/stage/theory"', '"/api/v1/competitions/{competitionId}/theory"')
        c = c.replace("public class TheoryStageController", "public class TheoryCompetitionController")
        c = c.replace("TheoryStageService", "TheoryCompetitionService")
        c = c.replace("theoryStageService", "theoryCompetitionService")
        with open(dst, 'w', encoding='utf-8') as f:
            f.write(c)
        os.remove(thf)
    if os.path.exists(ctrl_dir):
        shutil.rmtree(ctrl_dir)

# Services: service/stage -> service
svc_stage_dir = os.path.join(COMPETITION_JAVA, "service/stage")
if os.path.exists(svc_stage_dir):
    service_defs = [
        ("ctf/CtfStageService.java", "service/ctf/CtfCompetitionService.java", "CtfStageService", "CtfCompetitionService", "ctf"),
        ("ctf/impl/CtfStageServiceImpl.java", "service/ctf/impl/CtfCompetitionServiceImpl.java", "CtfStageServiceImpl", "CtfCompetitionServiceImpl", "ctf"),
        ("awd/AwdStageService.java", "service/awd/AwdCompetitionService.java", "AwdStageService", "AwdCompetitionService", "awd"),
        ("awd/impl/AwdStageServiceImpl.java", "service/awd/impl/AwdCompetitionServiceImpl.java", "AwdStageServiceImpl", "AwdCompetitionServiceImpl", "awd"),
        ("awdp/AwdPlusStageService.java", "service/awdp/AwdPlusCompetitionService.java", "AwdPlusStageService", "AwdPlusCompetitionService", "awdp"),
        ("awdp/impl/AwdPlusStageServiceImpl.java", "service/awdp/impl/AwdPlusCompetitionServiceImpl.java", "AwdPlusStageServiceImpl", "AwdPlusCompetitionServiceImpl", "awdp"),
        ("isw/IswStageService.java", "service/isw/IswCompetitionService.java", "IswStageService", "IswCompetitionService", "isw"),
        ("isw/impl/IswStageServiceImpl.java", "service/isw/impl/IswCompetitionServiceImpl.java", "IswStageServiceImpl", "IswCompetitionServiceImpl", "isw"),
        ("theory/TheoryStageService.java", "service/theory/TheoryCompetitionService.java", "TheoryStageService", "TheoryCompetitionService", "theory"),
        ("theory/impl/TheoryStageServiceImpl.java", "service/theory/impl/TheoryCompetitionServiceImpl.java", "TheoryStageServiceImpl", "TheoryCompetitionServiceImpl", "theory"),
    ]
    for old_sub, new_sub, old_name, new_name, domain_name in service_defs:
        src = os.path.join(svc_stage_dir, old_sub)
        dst = os.path.join(COMPETITION_JAVA, new_sub)
        if os.path.exists(src):
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            with open(src, 'r', encoding='utf-8') as f:
                c = f.read()
            c = c.replace("package com.humolix.volur.competition.service.stage." + domain_name, "package com.humolix.volur.competition.service." + domain_name)
            c = c.replace("com.humolix.volur.competition.service.stage." + domain_name, "com.humolix.volur.competition.service." + domain_name)
            c = c.replace(old_name, new_name)
            if old_name == "CtfStageService":
                c = c.replace("List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long stageId, Long userId);", "List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long userId);")
            if old_name == "CtfStageServiceImpl":
                # Remove overloaded listChallenges with stageId
                c = re.sub(r'@Override\s+public\s+List<CtfCompetitionChallengeVO>\s+listChallenges\(Long competitionId,\s*Long userId\)\s*\{\s*return\s+listChallenges\(competitionId,\s*null,\s*userId\);\s*\}', '', c)
                c = c.replace("public List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long stageId, Long userId) {", "public List<CtfCompetitionChallengeVO> listChallenges(Long competitionId, Long userId) {")
            with open(dst, 'w', encoding='utf-8') as f:
                f.write(c)
            os.remove(src)
    shutil.rmtree(svc_stage_dir)

print("Stage directory and file restructuring completed successfully.")
