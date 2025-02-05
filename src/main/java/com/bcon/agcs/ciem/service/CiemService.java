package com.bcon.agcs.ciem.service;

import com.bcon.agcs.ciem.model.CiemModel;
import com.bcon.agcs.ciem.utils.FetchTypeEnum;
import com.bcon.agcs.ciem.utils.TargetEnum;

public interface CiemService {
    CiemModel getDataByUserName(TargetEnum targetName, FetchTypeEnum fetchType, String userName);
}
