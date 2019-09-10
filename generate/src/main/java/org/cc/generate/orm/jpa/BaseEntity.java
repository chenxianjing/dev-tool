package org.cc.generate.orm.jpa;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Date;

@Data
@MappedSuperclass
public class BaseEntity implements Serializable{


    private static final long serialVersionUID = 2973435427822259959L;
    /**
     * 创建人id
     */
    @Column(
            name = "created_by"
    )
    private String createdBy;

    /**
     * 创建时间
     */
    @Column(
            name = "created_time", insertable = false, updatable = false
    )
    private Date createdTime;

    /**
     * 修改人id
     */
    @Column(
            name = "updated_by"
    )
    private String updatedBy;

    /**
     * 修改时间
     */
    @Column(
            name = "updated_time", insertable = false, updatable = false
    )
    private Date updatedTime;

    /**
     * 删除标志:0已删除1未删除
     */
    @Column(
            name = "del_flag", insertable = false
    )
    private Integer delFlag;
}
