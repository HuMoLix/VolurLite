package com.humolix.volur.challenge.service;

import com.humolix.volur.challenge.entity.Submission;
import org.springframework.data.domain.Page;

public interface SubmissionService {

    Page<Submission> pageUserSubmissions(Long userId, Integer page, Integer size);

}
