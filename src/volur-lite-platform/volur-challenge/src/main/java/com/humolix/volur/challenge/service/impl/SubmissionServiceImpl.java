package com.humolix.volur.challenge.service.impl;

import com.humolix.volur.challenge.entity.Submission;
import com.humolix.volur.challenge.repository.SubmissionRepository;
import com.humolix.volur.challenge.service.SubmissionService;
import com.humolix.volur.framework.common.util.SortUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;

    @Override
    public Page<Submission> pageUserSubmissions(Long userId, Integer page, Integer size) {
        Pageable pageable = SortUtils.createPageable(page, size);
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId, pageable);
    }

}
