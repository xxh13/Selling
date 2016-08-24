package promotion.sunshine.dao.impl;

import common.sunshine.dao.BaseDao;
import common.sunshine.model.selling.event.GiftEvent;
import common.sunshine.utils.ResponseCode;
import common.sunshine.utils.ResultData;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import promotion.sunshine.dao.EventDao;

/**
 * Created by sunshine on 8/23/16.
 */
@Repository
public class EventDaoImpl extends BaseDao implements EventDao {
    private Logger logger = LoggerFactory.getLogger(EventDaoImpl.class);

	@Override
	public ResultData queryGiftEvent(Map<String, Object> condition) {
		ResultData result = new ResultData();
		condition = handle(condition);
		try {
			List<GiftEvent> list = sqlSession.selectList("promotion.event.query", condition);
			result.setData(list);
		} catch (Exception e) {
			logger.error(e.getMessage());
			result.setResponseCode(ResponseCode.RESPONSE_ERROR);
			result.setDescription(e.getMessage());
		} finally {
			return result;
		}
	}
}