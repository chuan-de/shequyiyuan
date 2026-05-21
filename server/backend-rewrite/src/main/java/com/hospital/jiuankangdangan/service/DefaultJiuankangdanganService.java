package com.hospital.jiuankangdangan.service;

import com.hospital.jiuankangdangan.domain.JiuankangdanganRecord;
import com.hospital.jiuankangdangan.domain.JiuankangdanganStatus;
import com.hospital.jiuankangdangan.dto.JiuankangdanganUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.jiuankangdangan.repository.JiuankangdanganRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultJiuankangdanganService implements JiuankangdanganService {
    private final JiuankangdanganRepository repository;

    public DefaultJiuankangdanganService(JiuankangdanganRepository repository) { this.repository = repository; }

    @Override
    @Transactional(readOnly = true)
    public List<JiuankangdanganRecord> list(String keyword, JiuankangdanganStatus status) {
        return repository.findAll().stream()
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> keyword == null || keyword.isBlank() ||
                        (r.getYonghuName() != null && r.getYonghuName().toLowerCase().contains(keyword.toLowerCase())) ||
                        (r.getJiuankangdanganName() != null && r.getJiuankangdanganName().toLowerCase().contains(keyword.toLowerCase())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JiuankangdanganRecord detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Jiuankangdangan not found"));
    }

    @Override
    public JiuankangdanganRecord create(JiuankangdanganUpsertRequest r, String actor) {
        return repository.save(new JiuankangdanganRecord(null, r.yonghuId(), r.yonghuName(), r.yonghuPhone(),
                r.yonghuIdNumber(), r.yonghuEmail(), r.jiuankangdanganName(), r.jiuankangdanganQita(),
                r.jiuankangdanganTypes(), r.insertTime(), r.jiuankangdanganContent(),
                JiuankangdanganStatus.DRAFT, r.version()));
    }

    @Override
    public JiuankangdanganRecord update(Long id, JiuankangdanganUpsertRequest r, String actor) {
        JiuankangdanganRecord c = detail(id);
        return repository.save(new JiuankangdanganRecord(c.getId(), r.yonghuId(), r.yonghuName(), r.yonghuPhone(),
                r.yonghuIdNumber(), r.yonghuEmail(), r.jiuankangdanganName(), r.jiuankangdanganQita(),
                r.jiuankangdanganTypes(), r.insertTime(), r.jiuankangdanganContent(),
                c.getStatus(), c.getVersion()));
    }

    @Override
    public JiuankangdanganRecord changeStatus(Long id, JiuankangdanganStatus status, String actor) {
        JiuankangdanganRecord c = detail(id);
        return repository.save(new JiuankangdanganRecord(c.getId(), c.getYonghuId(), c.getYonghuName(), c.getYonghuPhone(),
                c.getYonghuIdNumber(), c.getYonghuEmail(), c.getJiuankangdanganName(), c.getJiuankangdanganQita(),
                c.getJiuankangdanganTypes(), c.getInsertTime(), c.getJiuankangdanganContent(),
                status, c.getVersion()));
    }
}
