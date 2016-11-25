package org.houxg.leamonax.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.QueryModel;
import com.raizlabs.android.dbflow.structure.BaseQueryModel;

import org.houxg.leamonax.database.AppDataBase;

@QueryModel(database = AppDataBase.class)
public class QueryTagRelationship extends BaseQueryModel{

    @Column(name = "tagId")
    String tagId;
    @Column(name = "userId")
    String UserId;
    @Column(name = "text")
    String text;
    @Column(name = "isDeleted")
    boolean isDeleted;
    @Column(name = "usn")
    int usn;
    @Column(name = "id")
    long id;
    @Column(name = "createdTime")
    long createdTime;
    @Column(name = "updatedTime")
    long updatedTime;
}
