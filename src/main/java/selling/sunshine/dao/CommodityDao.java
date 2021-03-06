package selling.sunshine.dao;

import common.sunshine.model.selling.goods.Goods4Customer;
import common.sunshine.model.selling.goods.Thumbnail;
import common.sunshine.pagination.DataTableParam;
import common.sunshine.utils.ResultData;

import java.util.List;
import java.util.Map;

/**
 * Created by sunshine on 4/8/16.
 */
public interface CommodityDao {
    ResultData insertGoods4Customer(Goods4Customer goods);

    ResultData queryGoods4Agent(Map<String, Object> condition);

    ResultData queryGoods4Customer(Map<String, Object> condition);

    ResultData queryGoods4CustomerByPage(Map<String, Object> condition, DataTableParam param);

    ResultData updateGoods4Customer(Goods4Customer goods);

    ResultData updateGoodsThumbnail(List<Thumbnail> thumbnails);

    ResultData insertGoodsThumbnail(Thumbnail thumbnail);
    
    ResultData deleteGoodsThumbnail(String thumbnailId);
    
    ResultData queryThumbnail(Map<String, Object> condition);
    
    ResultData queryThumbnail();
}
