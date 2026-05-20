package com.hospital.bingli.service;

import com.hospital.bingli.domain.BingliRecord;
import com.hospital.bingli.domain.BingliStatus;
import com.hospital.bingli.dto.BingliUpsertRequest;
import com.hospital.bingli.repository.BingliRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultBingliService implements BingliService {
    private final BingliRepository repository;

    public DefaultBingliService(BingliRepository repository) { this.repository = repository; }

    @Override
    public List<BingliRecord> list(String keyword, BingliStatus status) { return repository.findAll(keyword, status); }

    @Override
    public BingliRecord detail(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Bingli not found")); }

    @Override
    public BingliRecord create(BingliUpsertRequest r, String actor) {
        return repository.save(new BingliRecord(null, r.yishengId(), r.yishengUuidNumber(), r.yishengName(), r.yishengPhone(), r.yishengIdNumber(),
                r.yishengEmail(), r.yonghuId(), r.yonghuName(), r.yonghuPhone(), r.yonghuIdNumber(), r.yonghuEmail(), r.bingliUuidNumber(),
                r.bingliName(), r.bingliBingqing(), r.jianchaxiangmu(), r.jianchajieguo(), BingliStatus.DRAFT, r.version()));
    }

    @Override
    public BingliRecord update(Long id, BingliUpsertRequest r, String actor) {
        BingliRecord c = detail(id);
        return repository.save(new BingliRecord(c.id(), r.yishengId(), r.yishengUuidNumber(), r.yishengName(), r.yishengPhone(), r.yishengIdNumber(),
                r.yishengEmail(), r.yonghuId(), r.yonghuName(), r.yonghuPhone(), r.yonghuIdNumber(), r.yonghuEmail(), r.bingliUuidNumber(),
                r.bingliName(), r.bingliBingqing(), r.jianchaxiangmu(), r.jianchajieguo(), c.status(), c.version()));
    }

    @Override
    public BingliRecord changeStatus(Long id, BingliStatus status, String actor) {
        BingliRecord c = detail(id);
        return repository.save(new BingliRecord(c.id(), c.yishengId(), c.yishengUuidNumber(), c.yishengName(), c.yishengPhone(), c.yishengIdNumber(),
                c.yishengEmail(), c.yonghuId(), c.yonghuName(), c.yonghuPhone(), c.yonghuIdNumber(), c.yonghuEmail(), c.bingliUuidNumber(),
                c.bingliName(), c.bingliBingqing(), c.jianchaxiangmu(), c.jianchajieguo(), status, c.version()));
    }
}
