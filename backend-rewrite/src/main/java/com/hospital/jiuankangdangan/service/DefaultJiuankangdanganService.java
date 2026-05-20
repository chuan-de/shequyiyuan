package com.hospital.jiuankangdangan.service;

import com.hospital.jiuankangdangan.domain.JiuankangdanganRecord;
import com.hospital.jiuankangdangan.domain.JiuankangdanganStatus;
import com.hospital.jiuankangdangan.dto.JiuankangdanganUpsertRequest;
import com.hospital.jiuankangdangan.repository.JiuankangdanganRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultJiuankangdanganService implements JiuankangdanganService {
    private final JiuankangdanganRepository repository;

    public DefaultJiuankangdanganService(JiuankangdanganRepository repository) { this.repository = repository; }

    @Override public List<JiuankangdanganRecord> list(String keyword, JiuankangdanganStatus status) { return repository.findAll(keyword, status); }
    @Override public JiuankangdanganRecord detail(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Jiuankangdangan not found")); }

    @Override
    public JiuankangdanganRecord create(JiuankangdanganUpsertRequest r, String actor) {
        return repository.save(new JiuankangdanganRecord(null, r.yonghuId(), r.yonghuName(), r.yonghuPhone(), r.yonghuIdNumber(), r.yonghuEmail(),
                r.jiuankangdanganName(), r.jiuankangdanganQita(), r.jiuankangdanganTypes(), r.insertTime(), r.jiuankangdanganContent(),
                JiuankangdanganStatus.DRAFT, r.version()));
    }

    @Override
    public JiuankangdanganRecord update(Long id, JiuankangdanganUpsertRequest r, String actor) {
        JiuankangdanganRecord c = detail(id);
        return repository.save(new JiuankangdanganRecord(c.id(), r.yonghuId(), r.yonghuName(), r.yonghuPhone(), r.yonghuIdNumber(), r.yonghuEmail(),
                r.jiuankangdanganName(), r.jiuankangdanganQita(), r.jiuankangdanganTypes(), r.insertTime(), r.jiuankangdanganContent(),
                c.status(), c.version()));
    }

    @Override
    public JiuankangdanganRecord changeStatus(Long id, JiuankangdanganStatus status, String actor) {
        JiuankangdanganRecord c = detail(id);
        return repository.save(new JiuankangdanganRecord(c.id(), c.yonghuId(), c.yonghuName(), c.yonghuPhone(), c.yonghuIdNumber(), c.yonghuEmail(),
                c.jiuankangdanganName(), c.jiuankangdanganQita(), c.jiuankangdanganTypes(), c.insertTime(), c.jiuankangdanganContent(),
                status, c.version()));
    }
}
