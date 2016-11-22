package org.houxg.leamonax.network.api;

import org.houxg.leamonax.model.Notebook;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface NotebookApi {

    @GET("notebook/getSyncNotebooks")
    Call<List<Notebook>> getSyncNotebooks(@Query("afterUsn") int afterUsn, @Query("maxEntry") int maxEntry);

    @GET("notebook/getNotebooks")
    Call<List<Notebook>> getNotebooks();

    @POST("notebook/addNotebook")
    Call<Notebook> addNotebook(@Query("title") String title, @Query("parentNotebookId") String parentId);
}
