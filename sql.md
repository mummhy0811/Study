# MY SQL 기초


## <테이블 생성>
```
CREATE TABLE 테이블명(열_이름 데이터_타입 조건, 열_이름 데이터_타입 조건 ..);
```
<BR>

## <데이터 변경 문법 - 삽입, 업데이트, 삭제>
- INSERT   
	-> 테이블에 데이터를 삽입하는 명령   
    ```
	INSERT INTO 테이블명 (열1, 열2 .. ) VALUES 값1, 값2 ..;
    ```
	열 이름 생략 가능. 생략할 경우에 값들의 순서와 개수는 테이블의 열 순서 및 개수와 순서가 같아야 함   
	> AUTO INCREMENT   
		> - INSERT문을 실행할 때마다 자동으로 숫자값을 1부터 1씩 증가시켜줌   
		> - CREATE문에서 옵션을 입력하여 설정할 수 있음.   
		<pre>
        <code> CREATE TABLE 테이블명(열_이름 데이터_타입 조건 AUTO_INCREMENT PRIMARY KEY, 열_이름 데이터_타입 조건 ..);   </pre></code>
		> - ALTER TABLE을 이용하여 시작 값을 조정할 수 있음   
        <pre><code> ALTER TABLE 테이블명 AUTO_INCREMENT=원하는값;   </pre></code>
		> - 증가하는 양을 조정할 수 있음.   
        <pre><code> SET @@auto_increment_increment=원하는값;   </pre></code>


- INSERT INTO ~ SELECT   
	-> 조회한 값을 한 번에 입력하는 방법   
	
    ```
    INSERT INTO 삽입할_테이블명 SELECT 열이름 FROM 조회_테이블명;
    ```
     
- UPDATE	   
	-> 기존에 입력되어 있는 값을 수정   
    ```
	UPDATE 테이블명 SET 열1=값1, 열2=값2 .. WHERE 조건;
    ```
	** 업데이트가 안 되는 경우 워크벤치 설정을 풀어줘야 함. (EDIT-PREFERENCE-SQL EDITOR-SAFEUPDATE체크 해제-워크벤치 재실행)
	WHERE절을 뺄 경우 테이블 해당 열의 모든 값이 설정값으로 변경됨(주의 필요)
- DELETE   
	->테이블의 행데이터를 삭제
    ```
	ex) DELETE FROM 테이블명 WHERE 조건;
    ```

<BR><BR>

## <SELECT문>
```
SELECT 열_이름 FROM 테이블 이름 WHERE 조건식 GROUP BY 열_이름 HAVING 조건식 ORDER BY 열_이름 LIMIT 숫자;
```

### SELECT 열 이름 FROM 테이블 이름;    
- 테이블의 어떤 열을 사용하겠다. 
- 테이블 전체를 사용하고싶을 경우엔 *로 표시
- SELECT한 테이블이 db에 없으면 오류 발생.

### WHERE   
- 조건을 주는 코드
    ```
    SELECT 열 이름 FROM 테이블 이름 WHERE 조건; 
    ```
- AND(둘 다 만족)또는 OR(둘 중 하나만 만족)을 사용해서 조건 여러 개 사용 가능
- BETWEEN    
-> 숫자 범위 조건일 경우 BETWEEN 조건1 AND 조건2 의 형태로 사용 가능   
    ``` 
    //동일한 코드
    WHERE height >= 163 AND height <= 165; 
    WHERE height BETWEEN 163 AND 165;
    ```   
- IN   
-> 숫자 범위가 아니고, 여러 개 중 하나 만족일 경우 IN사용 가능
	```
    //동일한 코드
    WHERE addr = '경기' OR addr = '전남' OR addr = '경남'; 
    WHERE addr IN('경기', '전남', '경남');
    ```
- LIKE    
-> 문자를 비교. 정확하지 않은 글자를 조건으로 줄 때 사용.    
'_'는 한 글자 '%'는 여러글자를 의미
    ``` 	
    WHERE mem_name LIKE '우%'; // '우'로 시작하는 회원 이름 검색. 뒤는 상관 없음
   	WHERE mem_name LIKE '__핑크'; // 미지정 한 글자당 _ 하나로 표시
    ```

- db이름.테이블 이름    
	-> 테이블이 어디 db에 있는지 명시하는 방법.    
	다른 db를 잠시 use할 때 사용. 그러나 보통 여러 db를 한 번에 사용하는 경우가 많이 없기 때문에 잘 사용하지는 않음 
- Alias    
	-> 별칭을 주는 방법. 띄어쓰기하고 뒤에 별칭을 적어주면 됨   
	별칭에 띄어쓰기가 있을 경우 큰따옴표로 묶어주기   

	```
    SELECT 열 이름 별칭, 열 이름2 별칭2 FROM 테이블 이름; 
    ```
    


### ORDER BY   
-  결과값이나 개수에 영향을 미치지 않고, 출력에만 영향   
	* ASC    
    -> 오름차순 정렬. 기본적으로 오름차순이기 때문에 보통 생략
	* DESC   
    -> 내림차순 정렬
	동률일 경우 해결할 조건을 여러 개 줄 수 있음. 앞에 나온 조건이 우선적으로 처리됨
	```
    ORDER BY height DESC, debut_date ASC; 
        -> 키를 기준으로 정렬 후 동률인 열에 대해 데뷔일 기준 재정렬
    ```
### LIMIT   
- 출력할 행 개수 정해줌   
- ```LIMIT a, b; ```로 할 경우 a번째부터 b개 행만큼 출력
### DISTINCT   
- 중복된 데이터를 하나로만 보여줌  
    ```
        SELECT DISTINCT addr FROM member; 
            -> 멤버 테이블에 있는 주소 열을 중복되지 않게 출력
    ```

### GROUP BY   
- 그룹으로 묶어주는 역할   
	SUM() - 합계를 구함   
	AVG() - 평균을 구함   
	MIN() - 최소값을 구함   
	MAX() - 최대값을 구함   
	COUNT() - 행 개수를 셈 -> 열 이름을 ()안에 적어줄 경우 NULL값을 제외한 나머지 행을 count   
	COUNT(DISTINCT) - 중복을 제외하여 행 개수를 셈   
    ```
	SELECT mem_id, SUM(amount) FROM buy GROUP BY mem_id; 
        -> buy테이블의 member_id열을 선택하는데, mem_id별로 구매 양 합계를 구해서 출력
    ```
	
### HAVING    
- 그룹 함수에서 조건을 쓸 경우 WHERE절이 아닌 HAVING에 넣어줘야 함.   
    ```
    SELECT mem_id, SUM(amount*amount) FROM buy GROUP BY mem_id HAVING SUM(amount*amount)>1000 ; 
        ->  buy테이블의 member_id열을 선택하는데, mem_id별로 총 구매 금액을 구해서 1000이 넘는 행만 출력
    ```

<BR><BR>

## <데이터 형식>
- 크게 숫자형, 문자형, 날짜형으로 나뉨
### 숫자형
-  정수형   
  -> 소수점이 없는 숫자	   
  -> UNSIGNED 예약어를 사용해서 0부터 시작하는 양수로 만들 수 있음.
    <table>
        <tr>
        <td>데이터 형식</td><td>바이트 수</td><td>숫자 범위</td>
    </tr>
    <tr>
        <td >TINYINT<td>1<td>-128~127 </td>
    </tr>
    <tr>
        <td>SMALLINT<td> 2<td>-32768~32767 </td>
    </tr>
    <tr>
        <td >INT<td>4<td>약 -21억~ +21억</td>
    </tr>
    <tr>
    <td>BIGINT<td>8<td>약 -900경 ~ +900경</td>
    </tr>
    </table>
- 실수형   
    -> 소수점이 있는 숫자
    <table>
    <tr>
    <td>데이터 형식</td><td>바이트 수<td> 설명</td>
    </tr>
    <tr>
        <td >FLOAT<td>4<td>소수점 아래 7자리까지 표현</td>
    </tr>
    <tr>
        <td>DOUBLE<td>8<td>소수점 아래 15자리까지 표현</td>
    </tr>
    </TABLE>

### 문자형   
- 입력할 최대 글자수를 정해야 함   
    <table>
        <tr>
        <td>데이터 형식</td><td>바이트 수</td>
    </tr>
    <tr>
        <td >CHAR(개수)<td>1~255</td>
    </tr>
    <tr>
        <td>VARCHAR(개수)<td>1~16383</td>
    </tr>

    </table>
- CHAR의 경우 최대 글자수만큼 무조건 공간을 차지하기 때문에 VARCHAR을 사용하는 것이 공간을 더 효율적으로 운영 가능
- CHAR이 속도가 더 빠름

### 날짜형
- 날짜 및 시간을 저장할 때 사용
    <table>
    <tr>
    <td>데이터 형식</td><td>바이트 수<td> 설명</td>
    </tr>
    <tr>
        <td >DATE<td>3<td>날짜만 저장. YYYY-MM-DD형식</td>
    </tr>
    <tr>
        <td>TIME<td>3<td>시간만 저장. HH:MM:SS형식</td>
    </tr>
        <tr>
        <td>DATETIME<td>8<td>날짜와 시간 저장. YYYY-MM-DD HH:MM:SS 형식</td>
    </tr>
    </TABLE>

<BR><BR>
## <변수>
```
SET @변수이름 = 값;         //변수 선언과 동시에 값 대입
SELECT @변수이름;          //변수 값 출력
```
- 영구저장되는 것이 아니라 임시저장
- SELECT문의 LIMIT에는 사용할 수 없기 때문에, PREPARE과 EXECUTE문 사용
```
SET 변수명=값;
PREPARE 이름 FROM '원하는 구문 ?';
EXECUTE 이름 USING @변수명;

//예제
SET count=1;
PREPARE abc FROM 'SELECT name, height FROM table_A ORDER BY height LIMIT ?';
EXECUTE abc USING @count;
//물음표 자리에 USING뒤 변수가 들어감
```
