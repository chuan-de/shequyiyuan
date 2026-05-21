package com.hospital.bingli.service;

import com.hospital.bingli.domain.BingliRecord;
import com.hospital.bingli.domain.BingliStatus;
import com.hospital.bingli.dto.BingliUpsertRequest;
import com.hospital.common.NotFoundException;
import com.hospital.bingli.repository.BingliRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultBingliService implements BingliService {
    private final BingliRepository repository;

    public DefaultBingliService(BingliRepository repository) { this.repository = repository; }

    @Override
    @Transactional(readOnly = true)
    public List<BingliRecord> list(String keyword, BingliStatus status) {
        return repository.findAll().stream()
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> keyword == null || keyword.isBlank() ||
                        (b.getBingliName() != null && b.getBingliName().toLowerCase().contains(keyword.toLowerCase())) ||
                        (b.getYonghuName() != null && b.getYonghuName().toLowerCase().contains(keyword.toLowerCase())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BingliRecord detail(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Bingli not found"));
    }

    @Override
    public BingliRecord create(BingliUpsertRequest r, String actor) {
        return repository.save(new BingliRecord(null, r.yishengId(), r.yishengUuidNumber(), r.yishengName(),
                r.yishengPhone(), r.yishengIdNumber(), r.yishengEmail(),
                r.yonghuId(), r.yonghuName(), r.yonghuPhone(), r.yonghuIdNumber(), r.yonghuEmail(),
                r.bingliUuidNumber(), r.bingliName(), r.bingliBingqing(),
                r.jianchaxiangmu(), r.jianchajieguo(), BingliStatus.DRAFT, r.version()));
    }

    @Override
    public BingliRecord update(Long id, BingliUpsertRequest r, String actor) {
        BingliRecord c = detail(id);
        return repository.save(new BingliRecord(c.getId(), r.yishengId(), r.yishengUuidNumber(), r.yishengName(),
                r.yishengPhone(), r.yishengIdNumber(), r.yishengEmail(),
                r.yonghuId(), r.yonghuName(), r.yonghuPhone(), r.yonghuIdNumber(), r.yonghuEmail(),
                r.bingliUuidNumber(), r.bingliName(), r.bingliBingqing(),
                r.jianchaxiangmu(), r.jianchajieguo(), c.getStatus(), c.getVersion()));
    }

    @Override
    public BingliRecord changeStatus(Long id, BingliStatus status, String actor) {
        BingliRecord c = detail(id);
        return repository.save(new BingliRecord(c.getId(), c.getYishengId(), c.getYishengUuidNumber(), c.getYishengName(),
                c.getYishengPhone(), c.getYishengIdNumber(), c.getYishengEmail(),
                c.getYonghuId(), c.getYonghuName(), c.getYonghuPhone(), c.getYonghuIdNumber(), c.getYonghuEmail(),
                c.getBingliUuidNumber(), c.getBingliName(), c.getBingliBingqing(),
                c.getJianchaxiangmu(), c.getJianchajieguo(), status, c.getVersion()));
    }
}
