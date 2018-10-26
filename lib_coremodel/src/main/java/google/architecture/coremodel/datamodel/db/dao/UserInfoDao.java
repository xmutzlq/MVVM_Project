package google.architecture.coremodel.datamodel.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import google.architecture.coremodel.datamodel.db.entity.UserInfoEntity;
import io.reactivex.Flowable;

/**
 * @author lq.zeng
 * @date 2018/4/17
 */

@Dao
public interface UserInfoDao {
    @Query("SELECT * FROM user_info_entity WHERE id = :id")
    Flowable<UserInfoEntity> loadUserInfoEntity(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserInfoEntity(UserInfoEntity userInfoEntity);
}
