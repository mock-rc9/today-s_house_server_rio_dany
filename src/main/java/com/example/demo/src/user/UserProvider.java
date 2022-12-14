package com.example.demo.src.user;

import com.example.demo.config.BaseException;
import com.example.demo.config.secret.Secret;
import com.example.demo.src.scrab.ScrabService;
import com.example.demo.src.scrab.model.ScrabItem;
import com.example.demo.src.user.model.*;
import com.example.demo.utils.AES128;
import com.example.demo.utils.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.example.demo.config.BaseResponseStatus.*;
import static java.lang.Boolean.*;

//Provider : Read의 비즈니스 로직 처리
@Service    // [Business Layer에서 Service를 명시하기 위해서 사용] 비즈니스 로직이나 respository layer 호출하는 함수에 사용된다.
            // [Business Layer]는 컨트롤러와 데이터 베이스를 연결
/**
 * Provider란?
 * Controller에 의해 호출되어 실제 비즈니스 로직과 트랜잭션을 처리: Read의 비즈니스 로직 처리
 * 요청한 작업을 처리하는 관정을 하나의 작업으로 묶음
 * dao를 호출하여 DB CRUD를 처리 후 Controller로 반환
 */
@RequiredArgsConstructor
public class UserProvider {


    // *********************** 동작에 있어 필요한 요소들을 불러옵니다. *************************
    private final UserDao userDao;
    private final JwtService jwtService;
    private final ScrabService scrabService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

//    @Autowired //readme 참고
//    public UserProvider(UserDao userDao, JwtService jwtService) {
//        this.userDao = userDao;
//        this.jwtService = jwtService; // JWT부분은 7주차에 다루므로 모르셔도 됩니다!
//    }
    // ******************************************************************************


    // 로그인(password 검사)
    public PostLoginRes logIn(PostLoginReq postLoginReq) throws BaseException {
        User user = userDao.getPwd(postLoginReq);
        System.out.println("user.getPassword() = " + user.getPassword());
        String password;
        try {
            password = new AES128(Secret.USER_INFO_PASSWORD_KEY).decrypt(user.getPassword()); // 암호화
            // 회원가입할 때 비밀번호가 암호화되어 저장되었기 떄문에 로그인을 할때도 암호화된 값끼리 비교를 해야합니다.
        } catch (Exception ignored) {
            throw new BaseException(PASSWORD_DECRYPTION_ERROR);
        }

        if (postLoginReq.getPassword().equals(password)) { //비말번호가 일치한다면 userIdx를 가져온다.
            Long userIdx = userDao.getPwd(postLoginReq).getUserIdx();
//            return new PostLoginRes(userIdx);
//  *********** 해당 부분은 7주차 - JWT 수업 후 주석해제 및 대체해주세요!  **************** //
//            String jwt = jwtService.getJwt();  //로그인 시 회원 가입 때 발급받은 JWT를 사용하도록 변경
            String jwt = jwtService.createJwt(userIdx);
            return new PostLoginRes(userIdx, jwt);
//  **************************************************************************

        } else { // 비밀번호가 다르다면 에러메세지를 출력한다.
            throw new BaseException(FAILED_TO_LOGIN);
        }
    }

    // 해당 이메일이 이미 User Table에 존재하는지 확인
    public int checkEmail(String email) throws BaseException {
        try {
            return userDao.checkEmail(email);
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // User들의 정보를 조회
    public List<GetUserRes> getUsers() throws BaseException {
        try {
            List<GetUserRes> getUserRes = userDao.getUsers();
            return getUserRes;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 해당 nickname을 갖는 User들의 정보 조회
    public List<GetUserRes> getUsersByNickname(String nickname) throws BaseException {
        try {
            List<GetUserRes> getUsersRes = userDao.getUsersByNickname(nickname);
            return getUsersRes;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public Long getUserIdxByEmail(String email) throws BaseException{
        try{
            return userDao.getUserIdxByEmail(email);
        }catch (Exception exception){
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public Boolean isExist(Long userIdx) throws BaseException{
        try{
            List<GetUserRes> getUserResList = userDao.getUsers();
            long count = getUserResList.stream().filter(getUserRes -> getUserRes.getUserIdx().equals(userIdx)).count();
            System.out.println("count = " + count);
            if (count > 0){
                return TRUE;
            }
            else{
                return FALSE;
            }
        }catch(Exception exception){
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // 해당 userIdx를 갖는 User의 정보 조회
    public GetUserRes getUser(Long userIdx) throws BaseException {
        try {
            GetUserRes getUserRes = userDao.getUser(userIdx);
            return getUserRes;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public GetMyProfileRes getMyprofile(Long userIdx) throws BaseException{
        try{
            GetMyProfileRes getMyProfileRes = userDao.getMyprofile(userIdx);
            List<ScrabItem> scrabItems = scrabService.getScrabItems(userIdx);
            getMyProfileRes.setScrabItemCount(scrabItems.size()); //scrab item 총 개수 세팅
            List<String> scrabItemImageUrls = new ArrayList<>();
            for (ScrabItem scrabItem : scrabItems) {
                scrabItemImageUrls.add(scrabItem.getImageUrl());
            }
            getMyProfileRes.setScrabItemImageUrls(scrabItemImageUrls);  //scrab item 이미지 url 세팅
            return getMyProfileRes;
        }catch(Exception exception){
            System.out.println("exception.getMessage() = " + exception.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public GetOtherProfileRes getOtherprofile(Long userIdx) throws BaseException{
        try{
            GetOtherProfileRes getOtherProfileRes = userDao.getOtherProfile(userIdx);
            List<ScrabItem> scrabItems = scrabService.getScrabItems(userIdx);
            getOtherProfileRes.setScrabItemCount(scrabItems.size()); //scrab item 총 개수 세팅
            List<String> scrabItemImageUrls = new ArrayList<>();
            for (ScrabItem scrabItem : scrabItems) {
                scrabItemImageUrls.add(scrabItem.getImageUrl());
            }
            getOtherProfileRes.setScrabItemImageUrls(scrabItemImageUrls);  //scrab item 이미지 url 세팅
            return getOtherProfileRes;
        }catch(Exception exception){
            System.out.println("exception.getMessage() = " + exception.getMessage());
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public List<GetFollowRes> getFollowings(Long userIdx) throws BaseException{
        try {
            List<GetFollowRes> getFollowRes = userDao.getFollowings(userIdx);
            return getFollowRes;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public List<GetFollowRes> getFollowers(Long userIdx) throws BaseException{
        try {
            List<GetFollowRes> getFollowRes = userDao.getFollowers(userIdx);
            return getFollowRes;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public GetUserRes getUserByEmail(String email) throws BaseException{
        try {
            GetUserRes getUserRes = userDao.getUserByEmail(email);
            return getUserRes;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }
}
