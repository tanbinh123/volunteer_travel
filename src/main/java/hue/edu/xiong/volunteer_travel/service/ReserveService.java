package hue.edu.xiong.volunteer_travel.service;

import hue.edu.xiong.volunteer_travel.core.Result;
import hue.edu.xiong.volunteer_travel.core.ResultGenerator;
import hue.edu.xiong.volunteer_travel.core.ServiceException;
import hue.edu.xiong.volunteer_travel.model.Hotel;
import hue.edu.xiong.volunteer_travel.model.User;
import hue.edu.xiong.volunteer_travel.model.UserHotel;
import hue.edu.xiong.volunteer_travel.repository.HotelRepository;
import hue.edu.xiong.volunteer_travel.repository.UserHotelRepository;
import hue.edu.xiong.volunteer_travel.repository.UserRepository;
import hue.edu.xiong.volunteer_travel.util.CookieUitl;
import hue.edu.xiong.volunteer_travel.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author Xiong YuSong
 * @Date 2019/6/5
 */
@Service
public class ReserveService {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserHotelRepository userHotelRepository;

    public Page<Hotel> reserveHotelListUI(String searchName, Pageable pageable) {
        //查询启用的酒店列表
        Page<Hotel> hotelPage = hotelRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            //status状态,查询状态为0,启动的酒店
            predicates.add((cb.equal(root.get("status"), 0)));
            //酒店name模糊查询
            if (!StringUtils.isEmpty(searchName)) {
                predicates.add((cb.like(root.get("name"), "%" + searchName + "%")));
            }
            query.where(predicates.toArray(new Predicate[]{}));
            query.orderBy(cb.desc(root.get("createDate")));
            return null;
        }, pageable);
        return hotelPage;
    }

    public Hotel findHotelById(String id) {
        return hotelRepository.findById(id).orElseThrow(() -> new ServiceException("酒店id错误!"));
    }

    public List<UserHotel> getReserveHotelByUser(HttpServletRequest request) {
        Cookie cookie = CookieUitl.get(request, "username");
        if (cookie == null) {
            throw new ServiceException("未能获得正确的用户名");
        }
        User user = userRepository.findUserByUsername(cookie.getValue());
        return userHotelRepository.findUserHotelsByUser(user);
    }

    @Transactional(rollbackFor = Exception.class  )
    public Result cancelReserve(HttpServletRequest request, String id) {
        Cookie cookie = CookieUitl.get(request, "username");
        if (cookie == null) {
            throw new ServiceException("用户没有登录!");
        }
        Hotel hotel = findHotelById(id);
        User user = userRepository.findUserByUsername(cookie.getValue());
        UserHotel userHotel = userHotelRepository.findUserHotelByHotelAndUser(hotel, user);
        //存在值就是取消预约.不存在值就是预约
        if (userHotel != null) {
            userHotelRepository.delete(userHotel);
        } else {
            UserHotel newUserHotel = new UserHotel();
            newUserHotel.setId(IdGenerator.id());
            newUserHotel.setCreateDate(new Date());
            newUserHotel.setUser(user);
            newUserHotel.setHotel(hotel);
            userHotelRepository.saveAndFlush(newUserHotel);
        }
        return ResultGenerator.genSuccessResult();
    }

    public Boolean isReserveHotel(HttpServletRequest request, String id) {
        Cookie cookie = CookieUitl.get(request, "username");
        if (cookie != null) {
            User user = userRepository.findUserByUsername(cookie.getValue());
            Hotel hotel = findHotelById(id);
            UserHotel userHotel = userHotelRepository.findUserHotelByHotelAndUser(hotel, user);
            //每个酒店只能预约一次
            if (userHotel != null) {
                return true;
            }
        }
        return false;
    }
}
