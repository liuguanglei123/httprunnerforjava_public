package com.hrun.component.Meta_data;

import lombok.Data;

@Data
public class ResultStat {
    private Long content_size;

    private Long response_time_ms;

    private Long elapsed_ms;

    public ResultStat(){

    }
}
