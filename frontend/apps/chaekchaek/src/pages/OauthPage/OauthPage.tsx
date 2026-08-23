import { useEffect } from 'react';

export const OauthPage = () => {
  useEffect(() => {
    const loadData = async () => {
      // 내코드
      const res = await fetch('https://api.chaekchaek.com/api/v1/members/me', {
        credentials: 'include',
      });
      const data = await res.json();
      console.log(data);

      // 소낙눈 코드
      // const response = await fetch('https://api.chaekchaek.com/api/v1/members/me', {
      //   credentials: 'include',
      // });
      // const body = await response.json();
      // console.log(body);
      // console.log({
      //   status: response.status,
      //   contentType: response.headers.get('content-type'),
      //   body: JSON.stringify(body),
      // });
    };

    loadData();
  }, []);
  return null;
};
