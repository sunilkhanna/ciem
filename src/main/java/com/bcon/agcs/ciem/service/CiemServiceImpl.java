package com.bcon.agcs.ciem.service;

import com.bcon.agcs.ciem.component.CiemComponent;
import com.bcon.agcs.ciem.model.CiemModel;
import com.bcon.agcs.ciem.utils.FetchTypeEnum;
import com.bcon.agcs.ciem.utils.TargetEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CiemServiceImpl implements CiemService {

    @Autowired
    private CiemComponent ciemComponent;

    public CiemModel getDataByUserName(TargetEnum targetName, FetchTypeEnum fetchType, String userName) {
        return ciemComponent.getDataByUserName(targetName, fetchType, userName);
    }
}
