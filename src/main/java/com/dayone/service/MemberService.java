package com.dayone.service;

import com.dayone.exception.impl.AlreadyExistUserException;
import com.dayone.model.Auth;
import com.dayone.persist.MemberRepository;
import com.dayone.persist.entity.MemberEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("couldn't find user -> " + username));
    }

    /**
     * 회원가입 로직
     * 중복된 username이 있을 경우 AlreadyExistUserException 발생
     * 비밀번호는 암호화된 상태로 저장
     */

    public MemberEntity register(Auth.SignUp member) {
        // 아이디 중복 여부 확인
        boolean exists = memberRepository.existsByUsername(member.getUsername());
        if (exists) {
            throw new AlreadyExistUserException();
        }

        // 비밀번호 암호화 후 저장
        MemberEntity newMember = MemberEntity.builder()
                .username(member.getUsername())
                .password(passwordEncoder.encode(member.getPassword()))  // 비밀번호 암호화
                .roles(member.getRoles())
                .build();
        return memberRepository.save(newMember);
    }

    /**
     * 로그인 로직
     * 패스워드 검증 후, JWT 발급
     */
    public MemberEntity authenticate(Auth.SignIn member) {
        // 사용자 조회
        MemberEntity foundMember = memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        // 패스워드 확인
        if (!passwordEncoder.matches(member.getPassword(), foundMember.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return foundMember;
    }

}

