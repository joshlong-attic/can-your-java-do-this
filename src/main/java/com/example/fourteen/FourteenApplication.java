package com.example.fourteen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.List;
import java.util.Objects;

@SpringBootApplication
public class FourteenApplication {

	public static void main(String[] args) {
		SpringApplication.run(FourteenApplication.class, args);
	}
}

@Component
class Runner {

	private final PeopleService peopleService;

	Runner(PeopleService peopleService) {
		this.peopleService = peopleService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void ready() {
		Person elizabeth = this.peopleService.create("Elizabeth", EmotionalState.SAD);
		System.out.println(elizabeth.toString());
	}
}

@Service
class PeopleService {

	private final JdbcTemplate template;

	private final String findByIdSql =
		"""
			select * from PEOPLE
			 where ID =? 
			""";

	private final String insertSql =
		"""
			insert into PEOPLE(name,  emotional_state)
			 values (?, ?) 
			""";

	private final RowMapper<Person> reservationRowMapper =
		(rs, i) -> new Person(rs.getInt("id"), rs.getString("name"), rs.getInt("emotional_state"));

	PeopleService(JdbcTemplate template) {
		this.template = template;
	}

	public Person create(String name, EmotionalState state) {
		var parameters = List.of(
			new SqlParameter(Types.VARCHAR, "name"),
			new SqlParameter(Types.INTEGER, "emotional_state")
		);
		var pscf = new PreparedStatementCreatorFactory(this.insertSql, parameters) {
			{
				setReturnGeneratedKeys(true);
				setGeneratedKeysColumnNames("id");
			}
		};
		var emotionalState = switch (state) {
			case HAPPY -> 1;
			case SAD -> -1;
		};
		var psc = pscf.newPreparedStatementCreator(List.of(name, emotionalState));
		var kh = new GeneratedKeyHolder();
		this.template.update(psc, kh);
		var result = kh.getKey();
		if (result instanceof Integer id) {
			return findById(Objects.requireNonNull(id));
		}
		throw new RuntimeException("couldn't insert the record!");
	}

	public Person findById(Integer id) {
		return this.template.queryForObject(this.findByIdSql, new Object[]{id}, this.reservationRowMapper);
	}
}

record Person(Integer id, String name, int emotionalState) {
}

enum EmotionalState {
	SAD, HAPPY
}